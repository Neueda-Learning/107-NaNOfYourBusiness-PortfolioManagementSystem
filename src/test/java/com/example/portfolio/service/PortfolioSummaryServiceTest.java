package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioSummaryResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioSummaryServiceTest {

    @Mock
    private PortfolioItemRepository repository;

    private PortfolioSummaryService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioSummaryService(repository);
    }

    @Test
    void getSummary_returnsZerosForEmptyPortfolio() {
        when(repository.findAll()).thenReturn(List.of());

        PortfolioSummaryResponse response = service.getSummary();

        assertThat(response.getTotalValue()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalCost()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalGainLoss()).isEqualByComparingTo("0.00");
        assertThat(response.getTotalGainLossPercent()).isEqualByComparingTo("0");
        assertThat(response.getItemCount()).isZero();
        assertThat(response.getAllocationByType()).hasSize(3);
        assertThat(response.getAllocationByType()).allMatch(e -> e.getCount() == 0);
    }

    @Test
    void getSummary_calculatesTotalsAndAllocation() {
        PortfolioItem stock = new PortfolioItem();
        stock.setType(AssetType.STOCK);
        stock.setSymbolOrName("AAPL");
        stock.setQuantity(new BigDecimal("10"));
        stock.setPurchasePrice(new BigDecimal("100.00"));
        stock.setPurchaseDate(LocalDate.of(2025, 1, 1));
        stock.setCurrentPrice(new BigDecimal("120.00"));

        PortfolioItem bond = new PortfolioItem();
        bond.setType(AssetType.BOND);
        bond.setSymbolOrName("GOVT10Y");
        bond.setQuantity(new BigDecimal("5"));
        bond.setPurchasePrice(new BigDecimal("95.00"));
        bond.setPurchaseDate(LocalDate.of(2025, 1, 1));
        bond.setCurrentPrice(new BigDecimal("98.00"));

        when(repository.findAll()).thenReturn(List.of(stock, bond));

        PortfolioSummaryResponse response = service.getSummary();

        assertThat(response.getTotalValue()).isEqualByComparingTo("1690.00");
        assertThat(response.getTotalCost()).isEqualByComparingTo("1475.00");
        assertThat(response.getTotalGainLoss()).isEqualByComparingTo("215.00");
        assertThat(response.getTotalGainLossPercent()).isEqualByComparingTo("14.58");
        assertThat(response.getItemCount()).isEqualTo(2);
        assertThat(response.getAllocationByType())
                .extracting(PortfolioSummaryResponse.AllocationEntry::getType)
                .containsExactly("STOCK", "BOND", "MUTUAL_FUND");
        assertThat(response.getAllocationByType().get(0).getPercent()).isEqualByComparingTo("71.01");
        assertThat(response.getAllocationByType().get(0).getCount()).isEqualTo(1);
        assertThat(response.getAllocationByType().get(1).getPercent()).isEqualByComparingTo("28.99");
        assertThat(response.getAllocationByType().get(1).getCount()).isEqualTo(1);
        assertThat(response.getAllocationByType().get(2).getPercent()).isEqualByComparingTo("0");
        assertThat(response.getAllocationByType().get(2).getCount()).isEqualTo(0);
    }
}

