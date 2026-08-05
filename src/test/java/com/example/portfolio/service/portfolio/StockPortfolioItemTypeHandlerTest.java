package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.service.MarketDataService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockPortfolioItemTypeHandlerTest {

    @Test
    void applyCreateDefaults_setsPriceWhenCurrentPriceIsMissing() {
        MarketDataService marketDataService = mock(MarketDataService.class);
        StockPortfolioItemTypeHandler handler = new StockPortfolioItemTypeHandler(marketDataService);
        PortfolioItem item = new PortfolioItem();
        item.setSymbolOrName("TCS");

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
        item.setCurrentPrice(new BigDecimal("100.00"));

        handler.applyCreateDefaults(item);

        assertThat(item.getCurrentPrice()).isEqualByComparingTo("100.00");
    }
}

