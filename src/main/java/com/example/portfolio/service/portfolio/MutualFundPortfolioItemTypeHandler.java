package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.service.MarketDataService;
import org.springframework.stereotype.Component;

/**
 * Placeholder for upcoming mutual-fund-specific behavior.
 */
@Component
public class MutualFundPortfolioItemTypeHandler extends BasePortfolioItemTypeHandler {

    public MutualFundPortfolioItemTypeHandler(MarketDataService marketDataService) {
        super(marketDataService);
    }

    @Override
    public AssetType supportedType() {
        return AssetType.MUTUAL_FUND;
    }
}

