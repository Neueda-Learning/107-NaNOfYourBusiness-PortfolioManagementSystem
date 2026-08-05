package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioItemRequest;
import com.example.portfolio.dto.PortfolioItemResponse;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.mapper.PortfolioItemMapper;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.repository.PortfolioItemRepository;
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
    private MarketDataService marketDataService;

    private PortfolioItemService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioItemService(repository, new PortfolioItemMapper(), marketDataService);
    }

    @Test
    void create_stockWithoutCurrentPrice_fetchesAndPersistsMarketPrice() {
        PortfolioItemRequest request = new PortfolioItemRequest();
        request.setType(AssetType.STOCK);
        request.setSymbolOrName("tcs");
        request.setQuantity(BigDecimal.TEN);
        request.setPurchasePrice(new BigDecimal("100.00"));
        request.setPurchaseDate(LocalDate.of(2025, 1, 15));

        when(marketDataService.fetchPrice("TCS")).thenReturn(Optional.of(new BigDecimal("123.45")));
        when(repository.save(any(PortfolioItem.class))).thenAnswer(invocation -> {
            PortfolioItem saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.of(2026, 8, 4, 8, 0));
            saved.setUpdatedAt(LocalDateTime.of(2026, 8, 4, 8, 0));
            return saved;
        });

        PortfolioItemResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSymbolOrName()).isEqualTo("TCS");
        assertThat(response.getCurrentPrice()).isEqualByComparingTo("123.45");
        assertThat(response.getCurrentValue()).isEqualByComparingTo("1234.50");
        assertThat(response.getGainLoss()).isEqualByComparingTo("234.50");
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
        when(marketDataService.fetchPriceOrThrow("INFY.NS")).thenReturn(new BigDecimal("110.00"));

        PortfolioItemResponse response = service.refreshPrice(5L);

        verify(repository).updateCurrentPrice(5L, new BigDecimal("110.00"));
        assertThat(response.getCurrentPrice()).isEqualByComparingTo("110.00");
        assertThat(response.getCurrentValue()).isEqualByComparingTo("220.00");
        assertThat(response.getGainLoss()).isEqualByComparingTo("40.00");
    }
}

