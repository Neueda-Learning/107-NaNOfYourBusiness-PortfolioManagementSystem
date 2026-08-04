package com.example.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PortfolioItem {
    private Long id;
    private AssetType type;
    private String symbolOrName;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private LocalDate purchaseDate;
    private BigDecimal currentPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PortfolioItem() {
    }

    public PortfolioItem(Long id, AssetType type, String symbolOrName, BigDecimal quantity,
                         BigDecimal purchasePrice, LocalDate purchaseDate, BigDecimal currentPrice,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.type = type;
        this.symbolOrName = symbolOrName;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchaseDate = purchaseDate;
        this.currentPrice = currentPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
