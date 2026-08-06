package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioItemRequest;
import com.example.portfolio.dto.PortfolioItemResponse;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.mapper.PortfolioItemMapper;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.repository.PortfolioTradeRepository;
import com.example.portfolio.repository.PortfolioItemRepository;
import com.example.portfolio.service.portfolio.PortfolioItemTypeHandler;
import com.example.portfolio.service.portfolio.PortfolioItemTypeHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioItemServiceTest {

    @Mock
    private PortfolioItemRepository repository;

    @Mock
    private PortfolioItemTypeHandlerRegistry handlerRegistry;

    @Mock
    private PortfolioTradeRepository tradeRepository;

    @Mock
    private PortfolioItemTypeHandler stockHandler;

    private PortfolioItemService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioItemService(repository, tradeRepository, new PortfolioItemMapper(), handlerRegistry);
    }

    @Test
    void create_stockWithoutCurrentPrice_fetchesAndPersistsMarketPrice() {
        PortfolioItemRequest request = new PortfolioItemRequest();
        request.setType(AssetType.STOCK);
        request.setSymbolOrName("tcs");
        request.setQuantity(BigDecimal.TEN);
        request.setPurchasePrice(new BigDecimal("100.00"));
        request.setPurchaseDate(LocalDate.of(2025, 1, 15));

        when(handlerRegistry.resolve(AssetType.STOCK)).thenReturn(stockHandler);
        when(repository.save(any(PortfolioItem.class))).thenAnswer(invocation -> {
            PortfolioItem saved = invocation.getArgument(0);
            saved.setCurrentPrice(new BigDecimal("123.45"));
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.of(2026, 8, 4, 8, 0));
            saved.setUpdatedAt(LocalDateTime.of(2026, 8, 4, 8, 0));
            return saved;
        });

        PortfolioItemResponse response = service.create(request);

        verify(stockHandler).applyCreateDefaults(any(PortfolioItem.class));
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSymbolOrName()).isEqualTo("TCS");
        assertThat(response.getCurrentPrice()).isEqualByComparingTo("123.45");
        assertThat(response.getCurrentValue()).isEqualByComparingTo("1234.50");
        assertThat(response.getGainLoss()).isEqualByComparingTo("234.50");
    }

    @Test
    void create_stockAlreadyHeld_mergesIntoExistingHoldingWithWeightedAveragePrice() {
        PortfolioItem existing = new PortfolioItem();
        existing.setId(21L);
        existing.setType(AssetType.STOCK);
        existing.setSymbolOrName("AAPL");
        existing.setQuantity(new BigDecimal("10"));
        existing.setPurchasePrice(new BigDecimal("100.00"));
        existing.setPurchaseDate(LocalDate.of(2025, 1, 1));
        existing.setCurrentPrice(new BigDecimal("100.00"));

        PortfolioItemRequest request = new PortfolioItemRequest();
        request.setType(AssetType.STOCK);
        request.setSymbolOrName("AAPL");
        request.setQuantity(new BigDecimal("5"));
        request.setPurchasePrice(new BigDecimal("130.00"));
        request.setCurrentPrice(new BigDecimal("130.00"));
        request.setPurchaseDate(LocalDate.of(2026, 8, 6));

        when(handlerRegistry.resolve(AssetType.STOCK)).thenReturn(stockHandler);
        when(repository.findByTypeAndSymbolOrName(AssetType.STOCK, "AAPL"))
                .thenReturn(Optional.of(existing));

        PortfolioItemResponse response = service.create(request);

        // weighted average = (10*100 + 5*130) / 15 = 110.0000
        verify(repository).updateHoldingAfterTrade(
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("15")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("110.0000")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("130.00")),
                org.mockito.ArgumentMatchers.any());
        verify(tradeRepository).saveTrade(
                org.mockito.ArgumentMatchers.eq(existing),
                org.mockito.ArgumentMatchers.eq(com.example.portfolio.model.TradeSide.BUY),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("5")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("130.00")),
                org.mockito.ArgumentMatchers.any());
        verify(repository, org.mockito.Mockito.never()).save(any(PortfolioItem.class));
        assertThat(response.getId()).isEqualTo(21L);
        assertThat(response.getQuantity()).isEqualByComparingTo("15");
        assertThat(response.getPurchasePrice()).isEqualByComparingTo("110.0000");
    }

    @Test
    void findAll_withType_usesFilteredRepositoryMethod() {
        PortfolioItem stock = new PortfolioItem();
        stock.setId(7L);
        stock.setType(AssetType.STOCK);
        stock.setSymbolOrName("TCS.NS");
        stock.setQuantity(BigDecimal.ONE);
        stock.setPurchasePrice(new BigDecimal("10.00"));
        stock.setPurchaseDate(LocalDate.of(2025, 1, 1));
        stock.setCurrentPrice(new BigDecimal("12.00"));
        when(repository.findByType(AssetType.STOCK)).thenReturn(List.of(stock));

        List<PortfolioItemResponse> results = service.findAll(AssetType.STOCK);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getType()).isEqualTo(AssetType.STOCK);
    }

    @Test
    void delete_missingItem_throwsResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void refreshPrice_updatesStoredPriceAndReturnsUpdatedResponse() {
        PortfolioItem existing = new PortfolioItem();
        existing.setId(5L);
        existing.setType(AssetType.STOCK);
        existing.setSymbolOrName("INFY.NS");
        existing.setQuantity(new BigDecimal("2"));
        existing.setPurchasePrice(new BigDecimal("90.00"));
        existing.setPurchaseDate(LocalDate.of(2025, 2, 10));
        existing.setCurrentPrice(new BigDecimal("95.00"));
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(handlerRegistry.resolve(AssetType.STOCK)).thenReturn(stockHandler);
        when(stockHandler.resolveRefreshedPrice(existing)).thenReturn(new BigDecimal("110.00"));

        PortfolioItemResponse response = service.refreshPrice(5L);

        verify(repository).updateCurrentPrice(5L, new BigDecimal("110.00"));
        assertThat(response.getCurrentPrice()).isEqualByComparingTo("110.00");
        assertThat(response.getCurrentValue()).isEqualByComparingTo("220.00");
        assertThat(response.getGainLoss()).isEqualByComparingTo("40.00");
    }

    @Test
    void buy_increasesQuantityAndRecalculatesAverageCost() {
        PortfolioItem existing = new PortfolioItem();
        existing.setId(11L);
        existing.setType(AssetType.STOCK);
        existing.setSymbolOrName("AAPL");
        existing.setQuantity(new BigDecimal("10"));
        existing.setPurchasePrice(new BigDecimal("100.00"));
        existing.setPurchaseDate(LocalDate.of(2025, 1, 1));
        existing.setCurrentPrice(new BigDecimal("101.00"));

        when(repository.findById(11L)).thenReturn(Optional.of(existing));
        when(handlerRegistry.resolve(AssetType.STOCK)).thenReturn(stockHandler);
        when(stockHandler.resolveRefreshedPrice(existing)).thenReturn(new BigDecimal("130.00"));

        PortfolioItemResponse response = service.buy(11L, new BigDecimal("5"));

        verify(repository).updateHoldingAfterTrade(
                org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("15")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("110.0000")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("130.00")),
                org.mockito.ArgumentMatchers.any());
        verify(tradeRepository).saveTrade(
                org.mockito.ArgumentMatchers.eq(existing),
                org.mockito.ArgumentMatchers.eq(com.example.portfolio.model.TradeSide.BUY),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("5")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("130.00")),
                org.mockito.ArgumentMatchers.any());
        assertThat(response.getQuantity()).isEqualByComparingTo("15");
        assertThat(response.getPurchasePrice()).isEqualByComparingTo("110.0000");
    }

    @Test
    void sell_fullQuantity_deletesHoldingAndRecordsTrade() {
        PortfolioItem existing = new PortfolioItem();
        existing.setId(12L);
        existing.setType(AssetType.STOCK);
        existing.setSymbolOrName("MSFT");
        existing.setQuantity(new BigDecimal("3"));
        existing.setPurchasePrice(new BigDecimal("90.00"));
        existing.setPurchaseDate(LocalDate.of(2025, 1, 1));
        existing.setCurrentPrice(new BigDecimal("95.00"));

        when(repository.findById(12L)).thenReturn(Optional.of(existing));
        when(handlerRegistry.resolve(AssetType.STOCK)).thenReturn(stockHandler);
        when(stockHandler.resolveRefreshedPrice(existing)).thenReturn(new BigDecimal("120.00"));

        PortfolioItemResponse response = service.sell(12L, new BigDecimal("3"));

        verify(tradeRepository).saveTrade(
                org.mockito.ArgumentMatchers.eq(existing),
                org.mockito.ArgumentMatchers.eq(com.example.portfolio.model.TradeSide.SELL),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("3")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("120.00")),
                org.mockito.ArgumentMatchers.any());
        verify(repository).deleteById(12L);
        assertThat(response.getQuantity()).isEqualByComparingTo("0");
    }
}
