package com.example.portfolio.dto;

import java.math.BigDecimal;

public class MutualFundSummaryResponse {
    private Integer schemeCode;
    private String schemeName;
    private BigDecimal latestNav;

    public MutualFundSummaryResponse() {
    }

    public MutualFundSummaryResponse(Integer schemeCode, String schemeName, BigDecimal latestNav) {
        this.schemeCode = schemeCode;
        this.schemeName = schemeName;
        this.latestNav = latestNav;
    }

    public Integer getSchemeCode() {
        return schemeCode;
    }

    public void setSchemeCode(Integer schemeCode) {
        this.schemeCode = schemeCode;
    }

    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

    public BigDecimal getLatestNav() {
        return latestNav;
    }

    public void setLatestNav(BigDecimal latestNav) {
        this.latestNav = latestNav;
    }
}

