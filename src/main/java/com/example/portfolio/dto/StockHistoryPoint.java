package com.example.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StockHistoryPoint {
    private LocalDate date;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;

    public StockHistoryPoint() {
    }

    /** Convenience constructor for close-only callers (e.g. simple tests). */
    public StockHistoryPoint(LocalDate date, BigDecimal close) {
        this(date, null, null, null, close, null);
    }

    public StockHistoryPoint(LocalDate date, BigDecimal open, BigDecimal high,
                              BigDecimal low, BigDecimal close, BigDecimal volume) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
}

