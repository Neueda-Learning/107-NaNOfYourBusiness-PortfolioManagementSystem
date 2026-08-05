package com.example.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MutualFundHistoryPoint {
    private LocalDate date;
    private BigDecimal nav;

    public MutualFundHistoryPoint() {
    }

    public MutualFundHistoryPoint(LocalDate date, BigDecimal nav) {
        this.date = date;
        this.nav = nav;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getNav() {
        return nav;
    }

    public void setNav(BigDecimal nav) {
        this.nav = nav;
    }
}

