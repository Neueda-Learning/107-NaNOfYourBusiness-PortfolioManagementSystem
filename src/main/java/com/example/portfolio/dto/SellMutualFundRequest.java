package com.example.portfolio.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class SellMutualFundRequest {

    @NotNull(message = "portfolioItemId is required")
    @Positive(message = "portfolioItemId must be greater than 0")
    private Long portfolioItemId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;

    public Long getPortfolioItemId() {
        return portfolioItemId;
    }

    public void setPortfolioItemId(Long portfolioItemId) {
        this.portfolioItemId = portfolioItemId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

