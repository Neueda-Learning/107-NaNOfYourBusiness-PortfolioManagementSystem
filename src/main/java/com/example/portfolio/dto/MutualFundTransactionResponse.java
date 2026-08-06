package com.example.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single buy/sell transaction record for a specific mutual fund scheme.
 * Backed by the shared {@code portfolio_trade} table (same mechanism used for stocks),
 * filtered by scheme name + asset type = MUTUAL_FUND.
 */
public class MutualFundTransactionResponse {
    private Long id;
    private String side; // "BUY" | "SELL"
    private BigDecimal units;
    private BigDecimal nav;
    private BigDecimal amount;
    private LocalDateTime transactionDate;

    public MutualFundTransactionResponse() {
    }

    public MutualFundTransactionResponse(Long id, String side, BigDecimal units, BigDecimal nav,
                                          BigDecimal amount, LocalDateTime transactionDate) {
        this.id = id;
        this.side = side;
        this.units = units;
        this.nav = nav;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public void setUnits(BigDecimal units) {
        this.units = units;
    }

    public BigDecimal getNav() {
        return nav;
    }

    public void setNav(BigDecimal nav) {
        this.nav = nav;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}

