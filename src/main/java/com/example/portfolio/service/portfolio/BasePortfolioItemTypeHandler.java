package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.service.MarketDataService;

import java.math.BigDecimal;

/**
 * Shared default behavior to preserve existing refresh-price flow while specific
 * handlers incrementally add/override rules.
 */
public abstract class BasePortfolioItemTypeHandler implements PortfolioItemTypeHandler {

    private final MarketDataService marketDataService;

    protected BasePortfolioItemTypeHandler(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Override
    public void applyCreateDefaults(PortfolioItem item) {
        // Default: no-op. Type-specific handlers may enrich create data.
    }

    @Override
    public BigDecimal resolveRefreshedPrice(PortfolioItem item) {
        return marketDataService.fetchPriceOrThrow(item.getSymbolOrName());
    }

    protected MarketDataService marketDataService() {
        return marketDataService;
    }
}

