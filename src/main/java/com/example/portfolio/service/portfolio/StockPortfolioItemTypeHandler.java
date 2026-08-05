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
        // Auto-resolve market price for STOCK items.
        // If purchasePrice is absent the user submitted a quantity-only add request:
        // the current market price becomes the recorded purchase price.
        if (item.getPurchasePrice() == null || item.getCurrentPrice() == null) {
            var marketPriceOpt = marketDataService().fetchPrice(item.getSymbolOrName());

            if (marketPriceOpt.isEmpty()) {
                if (item.getPurchasePrice() == null) {
                    throw new IllegalArgumentException("Could not resolve market price for stock: " + item.getSymbolOrName());
                }
                return;
            }

            var marketPrice = marketPriceOpt.get();
            if (item.getPurchasePrice() == null) {
                item.setPurchasePrice(marketPrice);
            }
            if (item.getCurrentPrice() == null) {
                item.setCurrentPrice(marketPrice);
            }
        }
    }
}

