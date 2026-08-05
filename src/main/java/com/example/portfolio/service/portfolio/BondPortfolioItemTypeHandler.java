package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.service.MarketDataService;
import org.springframework.stereotype.Component;

/**
 * Placeholder for upcoming bond-specific behavior.
 */
@Component
public class BondPortfolioItemTypeHandler extends BasePortfolioItemTypeHandler {

    public BondPortfolioItemTypeHandler(MarketDataService marketDataService) {
        super(marketDataService);
    }

    @Override
    public AssetType supportedType() {
        return AssetType.BOND;
    }
}

