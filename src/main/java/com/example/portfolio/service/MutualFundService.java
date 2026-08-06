package com.example.portfolio.service;

import com.example.portfolio.client.MFAPIClient;
import com.example.portfolio.config.MutualFundCatalogue;
import com.example.portfolio.dto.BuyMutualFundRequest;
import com.example.portfolio.dto.MutualFundHistoryPoint;
import com.example.portfolio.dto.MutualFundHistoryResponse;
import com.example.portfolio.dto.MutualFundSummaryResponse;
import com.example.portfolio.dto.MutualFundTransactionResponse;
import com.example.portfolio.dto.SellMutualFundRequest;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.model.TradeSide;
import com.example.portfolio.repository.PortfolioItemRepository;
import com.example.portfolio.repository.PortfolioTradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class MutualFundService {

    private static final Logger log = LoggerFactory.getLogger(MutualFundService.class);

    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioTradeRepository portfolioTradeRepository;
    private final MFAPIClient mfapiClient;
    private final MutualFundCatalogue mutualFundCatalogue;

    public MutualFundService(PortfolioItemRepository portfolioItemRepository,
                             PortfolioTradeRepository portfolioTradeRepository,
                             MFAPIClient mfapiClient,
                             MutualFundCatalogue mutualFundCatalogue) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.portfolioTradeRepository = portfolioTradeRepository;
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

    private static final DateTimeFormatter MFAPI_DATE_FORMAT =
            new DateTimeFormatterBuilder().appendPattern("dd-MM-yyyy").toFormatter();

    /**
     * Get historical NAV data for a fund, optionally filtered by range.
     * range: "1M", "3M", "6M", "1Y", "ALL" (default ALL)
     */
    public MutualFundHistoryResponse getMutualFundHistory(Integer schemeCode, String range) {
        if (!mutualFundCatalogue.isSupported(schemeCode)) {
            throw new ResourceNotFoundException("Mutual fund is not supported");
        }

        String schemeName = mutualFundCatalogue.getSchemeName(schemeCode);
        Map<String, Object> mfapiResponse = mfapiClient.getMutualFundDetails(schemeCode);

        List<MutualFundHistoryPoint> points = new ArrayList<>();
        Object dataObj = mfapiResponse.get("data");
        if (dataObj instanceof List<?> dataList) {
            for (Object entry : dataList) {
                if (entry instanceof Map<?, ?> entryMap) {
                    Object dateObj = entryMap.get("date");
                    Object navObj = entryMap.get("nav");
                    if (dateObj != null && navObj != null) {
                        try {
                            LocalDate date = LocalDate.parse(dateObj.toString(), MFAPI_DATE_FORMAT);
                            BigDecimal nav = new BigDecimal(navObj.toString());
                            points.add(new MutualFundHistoryPoint(date, nav));
                        } catch (Exception ignore) {
                            // skip malformed entries
                        }
                    }
                }
            }
        }

        // Sort ascending by date (oldest first) for charting
        points.sort(Comparator.comparing(MutualFundHistoryPoint::getDate));

        // Apply range filter
        LocalDate cutoff = switch (range == null ? "ALL" : range.toUpperCase()) {
            case "1M" -> LocalDate.now().minusMonths(1);
            case "3M" -> LocalDate.now().minusMonths(3);
            case "6M" -> LocalDate.now().minusMonths(6);
            case "1Y" -> LocalDate.now().minusYears(1);
            default -> LocalDate.MIN;
        };

        List<MutualFundHistoryPoint> filtered = points.stream()
                .filter(p -> !p.getDate().isBefore(cutoff))
                .toList();

        log.info("Fetched {} NAV history points for scheme {} (range={})", filtered.size(), schemeCode, range);

        return new MutualFundHistoryResponse(schemeCode, schemeName, filtered);
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
        // Buying date is always set to the current date automatically — not user-supplied.
        LocalDate purchaseDate = LocalDate.now();

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

        // Record this purchase in the shared trade history table (per-fund transaction log)
        LocalDateTime executedAt = LocalDateTime.now();
        portfolioTradeRepository.saveTrade(item, TradeSide.BUY, units, currentNav, executedAt);

        // Return success message
        return Map.of(
                "message", "Mutual fund purchased successfully",
                "schemeCode", schemeCode,
                "schemeName", schemeName,
                "units", units,
                "nav", currentNav,
                "totalAmount", amount,
                "purchaseDate", purchaseDate,
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
        LocalDateTime executedAt = LocalDateTime.now();

        // Record this sale in the shared trade history table (per-fund transaction log)
        // Recorded before deletion so the trade log survives holding closure.
        portfolioTradeRepository.saveTrade(holding, TradeSide.SELL, unitsToSell, currentNav, executedAt);

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

    /**
     * Get the buy/sell transaction history for a specific mutual fund scheme,
     * most recent first. Backed by the shared portfolio_trade table.
     */
    public List<MutualFundTransactionResponse> getTransactionHistory(Integer schemeCode) {
        if (!mutualFundCatalogue.isSupported(schemeCode)) {
            throw new ResourceNotFoundException("Mutual fund is not supported");
        }
        String schemeName = mutualFundCatalogue.getSchemeName(schemeCode);

        return portfolioTradeRepository.findBySymbolAndType(schemeName, AssetType.MUTUAL_FUND).stream()
                .map(t -> new MutualFundTransactionResponse(
                        t.id(),
                        t.side().name(),
                        t.quantity(),
                        t.executionPrice(),
                        t.quantity().multiply(t.executionPrice()).setScale(2, RoundingMode.HALF_UP),
                        t.executedAt()
                ))
                .toList();
    }
}


