package com.example.portfolio.controller;

import com.example.portfolio.dto.StockCatalogItemResponse;
import com.example.portfolio.dto.StockQuoteResponse;
import com.example.portfolio.exception.ExternalApiException;
import com.example.portfolio.service.MarketDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    /**
     * GET /api/v1/market/supported-tickers
     * Returns the list of tickers the backend knows how to quote.
     */
    @GetMapping("/supported-tickers")
    public ResponseEntity<List<String>> getSupportedTickers() {
        return ResponseEntity.ok(marketDataService.getSupportedTickers());
    }

    @GetMapping("/stock-catalog")
    public ResponseEntity<List<StockCatalogItemResponse>> getStockCatalog() {
        return ResponseEntity.ok(marketDataService.getStockCatalog());
    }

    /**
     * GET /api/v1/market/quote?ticker=TSLA
     * Returns live quote for a ticker. 502 if upstream is unavailable.
     */
    @GetMapping("/quote")
    public ResponseEntity<StockQuoteResponse> getQuote(@RequestParam String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker parameter must not be blank");
        }
        return marketDataService.getQuote(ticker)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ExternalApiException(
                        "Unable to fetch live price for ticker: " + ticker.trim().toUpperCase()));
    }
}
