package com.example.portfolio.dto;

import java.util.List;

public class StockHistoryResponse {
    private String ticker;
    private String companyName;
    private List<StockHistoryPoint> history;

    public StockHistoryResponse() {
    }

    public StockHistoryResponse(String ticker, String companyName, List<StockHistoryPoint> history) {
        this.ticker = ticker;
        this.companyName = companyName;
        this.history = history;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<StockHistoryPoint> getHistory() {
        return history;
    }

    public void setHistory(List<StockHistoryPoint> history) {
        this.history = history;
    }
}

