package com.example.portfolio.dto;

public class StockCatalogItemResponse {

    private final String symbol;
    private final String companyName;
    private final String currency;

    public StockCatalogItemResponse(String symbol, String companyName, String currency) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currency = currency;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCurrency() {
        return currency;
    }
}

