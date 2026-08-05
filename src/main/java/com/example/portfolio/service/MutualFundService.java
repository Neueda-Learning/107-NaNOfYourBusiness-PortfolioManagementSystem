package com.example.portfolio.service;

import com.example.portfolio.client.MFAPIClient;
import com.example.portfolio.config.MutualFundCatalogue;
import com.example.portfolio.dto.BuyMutualFundRequest;
import com.example.portfolio.dto.MutualFundSummaryResponse;
import com.example.portfolio.dto.SellMutualFundRequest;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.repository.PortfolioItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MutualFundService {

    private static final Logger log = LoggerFactory.getLogger(MutualFundService.class);

    private final PortfolioItemRepository portfolioItemRepository;
    private final MFAPIClient mfapiClient;
    private final MutualFundCatalogue mutualFundCatalogue;

    public MutualFundService(PortfolioItemRepository portfolioItemRepository,
                             MFAPIClient mfapiClient,
                             MutualFundCatalogue mutualFundCatalogue) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.mfapiClient = mfapiClient;
        this.mutualFundCatalogue = mutualFundCatalogue;
    }

    /**
     * Get all supported mutual funds (30 funds)
     * Fetches latest NAV for each from MFAPI
     */
    public List<MutualFundSummaryResponse> getSupportedFunds() {
        return mutualFundCatalogue.getAllFunds().entrySet().stream()
                .map(entry -> {
                    try {
                        Map<String, Object> mfapiResponse = mfapiClient.getMutualFundDetails(entry.getKey());
                        BigDecimal latestNav = mfapiClient.extractLatestNav(mfapiResponse);
                        return new MutualFundSummaryResponse(entry.getKey(), entry.getValue(), latestNav);
                    } catch (Exception e) {
                        log.warn("Failed to fetch NAV for scheme code {}: {}", entry.getKey(), e.getMessage());
                        // Return with null NAV if MFAPI fails (graceful degradation)
                        return new MutualFundSummaryResponse(entry.getKey(), entry.getValue(), null);
                    }
                })
                .toList();
    }

    /**
     * Get mutual fund details from MFAPI (raw passthrough)
     */
    public Map<String, Object> getMutualFundDetails(Integer schemeCode) {
        // Validate fund is supported
        if (!mutualFundCatalogue.isSupported(schemeCode)) {
            throw new ResourceNotFoundException("Mutual fund is not supported");
        }

        return mfapiClient.getMutualFundDetails(schemeCode);
    }

    /**
     * Buy mutual fund using amount
     * Calculates units = amount / NAV
     * Creates portfolio item with type MUTUAL_FUND
     */
    public Map<String, Object> buyMutualFund(BuyMutualFundRequest request) {
        Integer schemeCode = request.getSchemeCode();

        // Validate fund is supported
        if (!mutualFundCatalogue.isSupported(schemeCode)) {
            throw new ResourceNotFoundException("Mutual fund is not supported");
        }

        String schemeName = mutualFundCatalogue.getSchemeName(schemeCode);
        LocalDate purchaseDate = request.getPurchaseDate() != null
                ? request.getPurchaseDate()
                : LocalDate.now();

        // Fetch latest NAV from MFAPI
        Map<String, Object> mfapiResponse = mfapiClient.getMutualFundDetails(schemeCode);
        BigDecimal currentNav = mfapiClient.extractLatestNav(mfapiResponse);

        // Calculate units: units = amount / NAV
        BigDecimal amount = request.getAmount();
        BigDecimal units = amount.divide(currentNav, 4, RoundingMode.HALF_UP);

        log.info("Buying {} units of {} (scheme code: {}) at NAV {}", 
                units, schemeName, schemeCode, currentNav);

        // Create portfolio item
        PortfolioItem item = new PortfolioItem();
        item.setType(AssetType.MUTUAL_FUND);
        item.setSymbolOrName(schemeName);
        item.setQuantity(units);
        item.setPurchasePrice(currentNav);
        item.setPurchaseDate(purchaseDate);
        item.setCurrentPrice(currentNav);

        portfolioItemRepository.save(item);

        log.info("Successfully purchased mutual fund. Portfolio item id: {}", item.getId());

        // Return success message
        return Map.of(
                "message", "Mutual fund purchased successfully",
                "schemeCode", schemeCode,
                "schemeName", schemeName,
                "units", units,
                "nav", currentNav,
                "totalAmount", amount,
                "portfolioItemId", item.getId()
        );
    }

    /**
     * Sell mutual fund using amount
     * Calculates units to sell = amount / current NAV
     * Updates or deletes portfolio item
     */
    public Map<String, Object> sellMutualFund(SellMutualFundRequest request) {
        Long portfolioItemId = request.getPortfolioItemId();

        // Fetch holding
        PortfolioItem holding = portfolioItemRepository.findById(portfolioItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio item not found with id: " + portfolioItemId));

        // Validate holding is MUTUAL_FUND
        if (holding.getType() != AssetType.MUTUAL_FUND) {
            throw new IllegalArgumentException(
                    "Portfolio item with id " + portfolioItemId + " is not a mutual fund");
        }

        // Fetch current NAV from MFAPI
        // Extract scheme code from symbol_or_name or use a different approach
        // For now, we'll fetch all supported funds and match by name
        BigDecimal currentNav = fetchCurrentNav(holding.getSymbolOrName());

        // Calculate units to sell
        BigDecimal amount = request.getAmount();
        BigDecimal unitsToSell = amount.divide(currentNav, 4, RoundingMode.HALF_UP);

        // Check if user has enough units
        if (unitsToSell.compareTo(holding.getQuantity()) > 0) {
            throw new IllegalArgumentException(
                    "Not enough units available to sell. Available: " + holding.getQuantity() + 
                    ", Requested: " + unitsToSell);
        }

        log.info("Selling {} units from portfolio item {}", unitsToSell, portfolioItemId);

        // Calculate remaining units
        BigDecimal remainingUnits = holding.getQuantity().subtract(unitsToSell);

        if (remainingUnits.compareTo(BigDecimal.ZERO) <= 0) {
            // Delete holding if all units sold (or if remaining is effectively zero/negative)
            portfolioItemRepository.deleteById(portfolioItemId);
            log.info("All units sold. Deleted portfolio item {}", portfolioItemId);
            return Map.of(
                    "message", "Mutual fund holding closed",
                    "portfolioItemId", portfolioItemId,
                    "unitsSold", unitsToSell,
                    "nav", currentNav,
                    "totalAmount", amount,
                    "remainingUnits", BigDecimal.ZERO
            );
        } else {
            // Update holding with remaining units
            holding.setQuantity(remainingUnits);
            holding.setCurrentPrice(currentNav);
            portfolioItemRepository.update(holding);
            log.info("Updated portfolio item {} with remaining units: {}", portfolioItemId, remainingUnits);
            return Map.of(
                    "message", "Mutual fund units sold successfully",
                    "portfolioItemId", portfolioItemId,
                    "unitsSold", unitsToSell,
                    "nav", currentNav,
                    "totalAmount", amount,
                    "remainingUnits", remainingUnits
            );
        }
    }

    /**
     * Fetch current NAV for a mutual fund by scheme name
     * Internal helper method
     */
    private BigDecimal fetchCurrentNav(String schemeName) {
        // Find scheme code that matches this scheme name
        Integer matchingSchemeCode = mutualFundCatalogue.getAllFunds().entrySet().stream()
                .filter(e -> e.getValue().equals(schemeName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mutual fund scheme not found: " + schemeName));

        Map<String, Object> mfapiResponse = mfapiClient.getMutualFundDetails(matchingSchemeCode);
        return mfapiClient.extractLatestNav(mfapiResponse);
    }
}


