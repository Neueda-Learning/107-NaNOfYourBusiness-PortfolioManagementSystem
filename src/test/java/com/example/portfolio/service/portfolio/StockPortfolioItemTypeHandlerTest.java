package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.service.MarketDataService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockPortfolioItemTypeHandlerTest {

    @Test
    void applyCreateDefaults_setsPriceWhenCurrentPriceIsMissing() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        StockPortfolioItemTypeHandler handler = new StockPortfolioItemTypeHandler(marketDataService);
        PortfolioItem item = new PortfolioItem();
        item.setSymbolOrName("TCS");
        item.setPurchasePrice(new BigDecimal("456.78")); // purchasePrice already provided

        when(marketDataService.fetchPrice("TCS")).thenReturn(Optional.of(new BigDecimal("456.78")));

        handler.applyCreateDefaults(item);

        assertThat(item.getCurrentPrice()).isEqualByComparingTo("456.78");
        verify(marketDataService).fetchPrice("TCS");
    }

    @Test
    void applyCreateDefaults_doesNotOverrideExistingCurrentPrice() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        StockPortfolioItemTypeHandler handler = new StockPortfolioItemTypeHandler(marketDataService);
        PortfolioItem item = new PortfolioItem();
        item.setSymbolOrName("TCS");
        item.setPurchasePrice(new BigDecimal("100.00"));
        item.setCurrentPrice(new BigDecimal("100.00"));

        handler.applyCreateDefaults(item);

        assertThat(item.getCurrentPrice()).isEqualByComparingTo("100.00");
        verify(marketDataService, never()).fetchPrice("TCS");
    }

    @Test
    void applyCreateDefaults_setsPurchasePriceAndCurrentPriceWhenBothMissing() {
        // Quantity-only add flow: user did not provide purchasePrice
        MarketDataService marketDataService = mock(MarketDataService.class);
        StockPortfolioItemTypeHandler handler = new StockPortfolioItemTypeHandler(marketDataService);
        PortfolioItem item = new PortfolioItem();
        item.setSymbolOrName("AAPL");
        // purchasePrice and currentPrice are null — simulates quantity-only add

        when(marketDataService.fetchPrice("AAPL")).thenReturn(Optional.of(new BigDecimal("195.40")));

        handler.applyCreateDefaults(item);

        assertThat(item.getPurchasePrice()).isEqualByComparingTo("195.40");
        assertThat(item.getCurrentPrice()).isEqualByComparingTo("195.40");
        verify(marketDataService).fetchPrice("AAPL");
    }

    @Test
    void applyCreateDefaults_doesNotOverrideProvidedPurchasePrice() {
        // User explicitly set purchasePrice; only currentPrice should be filled from market
        MarketDataService marketDataService = mock(MarketDataService.class);
        StockPortfolioItemTypeHandler handler = new StockPortfolioItemTypeHandler(marketDataService);
        PortfolioItem item = new PortfolioItem();
        item.setSymbolOrName("MSFT");
        item.setPurchasePrice(new BigDecimal("300.00")); // user-supplied purchase price

        when(marketDataService.fetchPrice("MSFT")).thenReturn(Optional.of(new BigDecimal("420.00")));

        handler.applyCreateDefaults(item);

        // purchasePrice must NOT be overridden
        assertThat(item.getPurchasePrice()).isEqualByComparingTo("300.00");
        // currentPrice filled from market
        assertThat(item.getCurrentPrice()).isEqualByComparingTo("420.00");
        verify(marketDataService).fetchPrice("MSFT");
    }
}

