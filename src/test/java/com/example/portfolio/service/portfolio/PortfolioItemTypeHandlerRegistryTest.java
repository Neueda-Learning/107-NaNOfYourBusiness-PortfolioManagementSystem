package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.AssetType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioItemTypeHandlerRegistryTest {

    @Test
    void resolve_returnsHandlerForType() {
        PortfolioItemTypeHandler stockHandler = mock(PortfolioItemTypeHandler.class);
        PortfolioItemTypeHandler bondHandler = mock(PortfolioItemTypeHandler.class);
        PortfolioItemTypeHandler mutualFundHandler = mock(PortfolioItemTypeHandler.class);
        when(stockHandler.supportedType()).thenReturn(AssetType.STOCK);
        when(bondHandler.supportedType()).thenReturn(AssetType.BOND);
        when(mutualFundHandler.supportedType()).thenReturn(AssetType.MUTUAL_FUND);

        PortfolioItemTypeHandlerRegistry registry = new PortfolioItemTypeHandlerRegistry(
                List.of(stockHandler, bondHandler, mutualFundHandler));

        assertThat(registry.resolve(AssetType.STOCK)).isSameAs(stockHandler);
        assertThat(registry.resolve(AssetType.BOND)).isSameAs(bondHandler);
        assertThat(registry.resolve(AssetType.MUTUAL_FUND)).isSameAs(mutualFundHandler);
    }

    @Test
    void constructor_throwsWhenHandlerMissing() {
        PortfolioItemTypeHandler stockHandler = mock(PortfolioItemTypeHandler.class);
        when(stockHandler.supportedType()).thenReturn(AssetType.STOCK);

        assertThatThrownBy(() -> new PortfolioItemTypeHandlerRegistry(List.of(stockHandler)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing portfolio item handlers");
    }
}

