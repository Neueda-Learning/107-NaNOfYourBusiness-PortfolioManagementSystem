package com.example.portfolio.dto;

import java.util.List;

public class MutualFundHistoryResponse {
    private Integer schemeCode;
    private String schemeName;
    private List<MutualFundHistoryPoint> history;

    public MutualFundHistoryResponse() {
    }

    public MutualFundHistoryResponse(Integer schemeCode, String schemeName, List<MutualFundHistoryPoint> history) {
        this.schemeCode = schemeCode;
        this.schemeName = schemeName;
        this.history = history;
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

    public List<MutualFundHistoryPoint> getHistory() {
        return history;
    }

    public void setHistory(List<MutualFundHistoryPoint> history) {
        this.history = history;
    }
}

