package com.example.portfolio.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class BuyMutualFundRequest {

    @NotNull(message = "schemeCode is required")
    @Positive(message = "schemeCode must be greater than 0")
    private Integer schemeCode;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;


    public Integer getSchemeCode() {
        return schemeCode;
    }

    public void setSchemeCode(Integer schemeCode) {
        this.schemeCode = schemeCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

