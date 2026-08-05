package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.service.MarketDataService;
import org.springframework.stereotype.Component;

/**
 * Stock-specific create behavior (auto-fill current price when omitted).
 */
@Component
public class StockPortfolioItemTypeHandler extends BasePortfolioItemTypeHandler {

    public StockPortfolioItemTypeHandler(MarketDataService marketDataService) {
        super(marketDataService);
    }

    @Override
    public AssetType supportedType() {
        return AssetType.STOCK;
    }

    @Override
    public void applyCreateDefaults(PortfolioItem item) {
        if (item.getCurrentPrice() == null) {
            marketDataService().fetchPrice(item.getSymbolOrName())
                    .ifPresent(item::setCurrentPrice);
        }
    }
}

