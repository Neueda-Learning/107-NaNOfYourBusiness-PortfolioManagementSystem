package com.example.portfolio.service;

import com.example.portfolio.exception.BondRedemptionException;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.repository.BondRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BondService {

    private static final Logger log = LoggerFactory.getLogger(BondService.class);

    private final BondRepository repository;
    private final WalletService walletService;

    public BondService(BondRepository repository, WalletService walletService) {
        this.repository = repository;
        this.walletService = walletService;
    }

    public List<BondResponse> getAllBonds() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    /**
     * All bonds in the database regardless of status (ACTIVE + REDEEMED),
     * used for the "Available Bonds" catalog on the frontend.
     */
    public List<BondResponse> getBondCatalog() {
        return repository.findAllAnyStatus().stream().map(this::toResponse).toList();
    }

    public BondResponse getBondDetails(Long id) {
        return toResponse(requireBondById(id));
    }

    public List<BondResponse> getRedeemedBonds() {
        return repository.findRedeemed().stream().map(this::toResponse).toList();
    }

    public List<BondResponse> searchBonds(String query,
                                          String issuer,
                                          String creditRating,
                                          LocalDate maturityFrom,
                                          LocalDate maturityTo) {
        if (maturityFrom != null && maturityTo != null && maturityFrom.isAfter(maturityTo)) {
            throw new IllegalArgumentException("maturityFrom must be before or equal to maturityTo");
        }

        return repository.search(query, issuer, creditRating, maturityFrom, maturityTo)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BondResponse buyBond(BondTradeRequest request) {
        validateTradeRequest(request);
        String normalizedSymbol = normalizeSymbol(request.symbol());
        BigDecimal buyAmount = request.quantity().multiply(request.purchasePrice());

        walletService.debitForBuy(buyAmount, AssetType.BOND, null, normalizedSymbol);

        BondRepository.BondRecord updated = repository.findBySymbol(normalizedSymbol)
                .map(existing -> repository.mergeBuy(
                        existing,
                        request.quantity(),
                        request.purchasePrice(),
                        request.purchaseDate(),
                        request.currentPrice(),
                        cleanText(request.issuer()),
                        request.faceValue(),
                        request.couponRate(),
                        cleanText(request.couponFrequency()),
                        request.maturityDate(),
                        cleanText(request.creditRating()),
                        request.yieldRate()))
                .orElseGet(() -> repository.saveNew(new BondRepository.BondRecord(
                        null,
                        normalizedSymbol,
                        request.quantity(),
                        request.purchasePrice(),
                        request.purchaseDate(),
                        request.currentPrice() != null ? request.currentPrice() : request.purchasePrice(),
                        cleanText(request.issuer()),
                        request.faceValue(),
                        request.couponRate(),
                        cleanText(request.couponFrequency()),
                        request.maturityDate(),
                        cleanText(request.creditRating()),
                        request.yieldRate(),
                        "ACTIVE",
                        null,
                        null,
                        null,
                        null
                )));

        log.info("Bond buy executed: symbol={}, quantity={}, purchasePrice={}, portfolioItemId={}",
                normalizedSymbol, request.quantity(), request.purchasePrice(), updated.id());

        return toResponse(updated);
    }

    @Transactional
    public BondResponse redeemBond(BondRedeemRequest request) {
        String normalizedSymbol = normalizeSymbol(request.symbol());

        BondRepository.BondRecord existing = repository.findAnyBySymbol(normalizedSymbol)
                .orElseThrow(() -> new ResourceNotFoundException("Bond not found for symbol: " + normalizedSymbol));

        if ("REDEEMED".equalsIgnoreCase(existing.status())) {
            String dateText = existing.redemptionDate() != null ? existing.redemptionDate().toString() : "an earlier date";
            log.warn("Bond redemption rejected: symbol={} already redeemed on {}", normalizedSymbol, dateText);
            throw new BondRedemptionException(
                    "BOND_ALREADY_REDEEMED",
                    "Bond is already redeemed (redeemed on " + dateText + ")"
            );
        }

        if (existing.maturityDate() == null) {
            log.warn("Bond redemption rejected: symbol={} has no maturity date", normalizedSymbol);
            throw new BondRedemptionException(
                    "BOND_MATURITY_DATE_MISSING",
                    "Bond has no maturity date set; redemption is not applicable"
            );
        }
        if (existing.maturityDate().isAfter(LocalDate.now())) {
            log.warn("Bond redemption rejected: symbol={} has not matured (maturityDate={})",
                    normalizedSymbol, existing.maturityDate());
            throw new BondRedemptionException(
                    "BOND_NOT_MATURED",
                    "Bond has not yet matured. Maturity date: " + existing.maturityDate());
        }

        BondRepository.BondRecord redeemed = repository.applyRedeem(existing);
        walletService.creditForSell(
                redeemed.redemptionValue(),
                AssetType.BOND,
                redeemed.id(),
                redeemed.symbol());
        log.info("Bond redeemed: symbol={}, portfolioItemId={}, redemptionValue={}",
                redeemed.symbol(), redeemed.id(), redeemed.redemptionValue());
        return toResponse(redeemed);
    }

    private BondRepository.BondRecord requireBondById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bond not found with id: " + id));
    }

    private BondResponse toResponse(BondRepository.BondRecord record) {
        BigDecimal marketPrice = record.currentPrice() != null ? record.currentPrice() : record.purchasePrice();
        BigDecimal currentValue = record.quantity().multiply(marketPrice);
        BigDecimal totalCost = record.quantity().multiply(record.purchasePrice());

        return new BondResponse(
                record.id(),
                record.symbol(),
                record.quantity(),
                record.purchasePrice(),
                record.purchaseDate(),
                record.currentPrice(),
                record.issuer(),
                record.faceValue(),
                record.couponRate(),
                record.couponFrequency(),
                record.maturityDate(),
                record.creditRating(),
                record.yieldRate(),
                record.status(),
                record.redemptionDate(),
                record.redemptionValue(),
                currentValue,
                currentValue.subtract(totalCost),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private void validateTradeRequest(BondTradeRequest request) {
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        if (request.purchasePrice() == null || request.purchasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("purchasePrice must be greater than 0");
        }
    }

    private String normalizeSymbol(String symbol) {
        String trimmed = cleanText(symbol);
        if (trimmed == null || trimmed.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        return trimmed.toUpperCase();
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public record BondResponse(Long id,
                               String symbol,
                               BigDecimal quantity,
                               BigDecimal purchasePrice,
                               LocalDate purchaseDate,
                               BigDecimal currentPrice,
                               String issuer,
                               BigDecimal faceValue,
                               BigDecimal couponRate,
                               String couponFrequency,
                               LocalDate maturityDate,
                               String creditRating,
                               BigDecimal yieldRate,
                               String status,
                               LocalDate redemptionDate,
                               BigDecimal redemptionValue,
                               BigDecimal currentValue,
                               BigDecimal gainLoss,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
    }

    public record BondTradeRequest(
            @NotBlank(message = "symbol is required")
            String symbol,

            @NotNull(message = "quantity is required")
            @DecimalMin(value = "0.0001", message = "quantity must be greater than 0")
            BigDecimal quantity,

            @NotNull(message = "purchasePrice is required")
            @DecimalMin(value = "0.0001", message = "purchasePrice must be greater than 0")
            BigDecimal purchasePrice,

            @NotNull(message = "purchaseDate is required")
            @PastOrPresent(message = "purchaseDate must not be in the future")
            LocalDate purchaseDate,

            @DecimalMin(value = "0.0001", message = "currentPrice must be greater than 0 when provided")
            BigDecimal currentPrice,

            String issuer,
            BigDecimal faceValue,
            BigDecimal couponRate,
            String couponFrequency,
            LocalDate maturityDate,
            String creditRating,
            BigDecimal yieldRate
    ) {
    }

    public record BondRedeemRequest(
            @NotBlank(message = "symbol is required")
            String symbol
    ) {
    }
}
