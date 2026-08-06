package com.example.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionResponse {

    private Long id;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String assetType;
    private Long portfolioItemId;
    private String symbolOrName;
    private LocalDateTime timestamp;

    public WalletTransactionResponse(Long id,
                                     String type,
                                     BigDecimal amount,
                                     BigDecimal balanceAfter,
                                     String assetType,
                                     Long portfolioItemId,
                                     String symbolOrName,
                                     LocalDateTime timestamp) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.assetType = assetType;
        this.portfolioItemId = portfolioItemId;
        this.symbolOrName = symbolOrName;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getAssetType() {
        return assetType;
    }

    public Long getPortfolioItemId() {
        return portfolioItemId;
    }

    public String getSymbolOrName() {
        return symbolOrName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

