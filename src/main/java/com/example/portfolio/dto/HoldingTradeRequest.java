package com.example.portfolio.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class HoldingTradeRequest {

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    private BigDecimal quantity;

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}

