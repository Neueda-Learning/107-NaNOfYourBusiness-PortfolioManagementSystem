package com.example.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockQuoteResponse {
    private String ticker;
    private BigDecimal price;
    private String currency;
    private LocalDateTime asOf;

    public StockQuoteResponse() {
    }

    public StockQuoteResponse(String ticker, BigDecimal price, String currency, LocalDateTime asOf) {
        this.ticker = ticker;
        this.price = price;
        this.currency = currency;
        this.asOf = asOf;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getAsOf() {
        return asOf;
    }

    public void setAsOf(LocalDateTime asOf) {
        this.asOf = asOf;
    }
}

