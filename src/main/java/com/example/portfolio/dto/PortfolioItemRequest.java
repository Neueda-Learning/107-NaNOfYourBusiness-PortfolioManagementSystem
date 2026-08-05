package com.example.portfolio.dto;

import com.example.portfolio.model.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioItemRequest {

    @NotNull(message = "type is required")
    private AssetType type;

    @NotBlank(message = "symbolOrName is required")
    @Size(max = 100, message = "symbolOrName must be 100 characters or fewer")
    private String symbolOrName;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    private BigDecimal quantity;

    /**
     * Required for BOND and MUTUAL_FUND.
     * For STOCK items this field is optional — the backend auto-fetches the current
     * market price and records it as the purchase price when omitted.
     */
    @Positive(message = "purchasePrice must be greater than 0")
    private BigDecimal purchasePrice;

    @NotNull(message = "purchaseDate is required")
    @PastOrPresent(message = "purchaseDate must not be in the future")
    private LocalDate purchaseDate;

    /** Optional for STOCK items — backend auto-fetches from market API if absent. */
    private BigDecimal currentPrice;

    public AssetType getType() {
        return type;
    }

    public void setType(AssetType type) {
        this.type = type;
    }

    public String getSymbolOrName() {
        return symbolOrName;
    }

    public void setSymbolOrName(String symbolOrName) {
        this.symbolOrName = symbolOrName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
}
