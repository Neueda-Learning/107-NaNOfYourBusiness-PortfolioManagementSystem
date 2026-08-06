package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stock-specific create behavior (auto-fill current price when omitted).
 */
@Component
public class StockPortfolioItemTypeHandler extends BasePortfolioItemTypeHandler {

    private static final Logger log = LoggerFactory.getLogger(StockPortfolioItemTypeHandler.class);

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
                    log.warn("Could not resolve market price for stock: {}", item.getSymbolOrName());
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
            log.debug("Auto-filled market price for stock: symbolOrName={}, price={}", item.getSymbolOrName(), marketPrice);
        }
    }
}

