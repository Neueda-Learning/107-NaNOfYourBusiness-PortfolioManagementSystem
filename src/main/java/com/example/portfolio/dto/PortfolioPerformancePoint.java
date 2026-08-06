package com.example.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single sampled point on the portfolio performance-over-time series.
 * `totalValue` and `totalCost` are aggregated across every holding held as of `date`.
 */
public class PortfolioPerformancePoint {
    private LocalDate date;
    private BigDecimal totalValue;
    private BigDecimal totalCost;

    public PortfolioPerformancePoint() {
    }

    public PortfolioPerformancePoint(LocalDate date, BigDecimal totalValue, BigDecimal totalCost) {
        this.date = date;
        this.totalValue = totalValue;
        this.totalCost = totalCost;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
}

