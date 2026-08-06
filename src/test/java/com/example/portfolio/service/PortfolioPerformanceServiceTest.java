package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioPerformanceResponse;
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
class PortfolioPerformanceServiceTest {

    @Mock
    private PortfolioItemRepository repository;

    private PortfolioPerformanceService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioPerformanceService(repository);
    }

    @Test
    void getPerformance_returnsEmptyPointsForEmptyPortfolio() {
        when(repository.findAll()).thenReturn(List.of());

        PortfolioPerformanceResponse response = service.getPerformance("1M");

        assertThat(response.getRange()).isEqualTo("1M");
        assertThat(response.getPoints()).isEmpty();
    }

    @Test
    void getPerformance_normalizesUnknownRangeToAll() {
        when(repository.findAll()).thenReturn(List.of());

        PortfolioPerformanceResponse response = service.getPerformance("bogus");

        assertThat(response.getRange()).isEqualTo("ALL");
    }

    @Test
    void getPerformance_startsAtPurchaseDateWithCostBasisValue() {
        PortfolioItem stock = new PortfolioItem();
        stock.setType(AssetType.STOCK);
        stock.setSymbolOrName("AAPL");
        stock.setQuantity(new BigDecimal("10"));
        stock.setPurchasePrice(new BigDecimal("100.00"));
        stock.setPurchaseDate(LocalDate.now().minusDays(10));
        stock.setCurrentPrice(new BigDecimal("150.00"));

        when(repository.findAll()).thenReturn(List.of(stock));

        PortfolioPerformanceResponse response = service.getPerformance("1M");

        // The requested "1M" start predates the holding's purchase date, so the series
        // is clamped to start exactly at the purchase date — priced at cost basis (no gain yet).
        var first = response.getPoints().get(0);
        assertThat(first.getDate()).isEqualTo(stock.getPurchaseDate());
        assertThat(first.getTotalValue()).isEqualByComparingTo("1000.00");
        assertThat(first.getTotalCost()).isEqualByComparingTo("1000.00");

        // The last point is always "today", priced exactly at currentPrice * quantity.
        var last = response.getPoints().get(response.getPoints().size() - 1);
        assertThat(last.getDate()).isEqualTo(LocalDate.now());
        assertThat(last.getTotalValue()).isEqualByComparingTo("1500.00");
        assertThat(last.getTotalCost()).isEqualByComparingTo("1000.00");
    }

    @Test
    void getPerformance_excludesLaterHoldingBeforeItsOwnPurchaseDate() {
        PortfolioItem early = new PortfolioItem();
        early.setType(AssetType.STOCK);
        early.setSymbolOrName("TCS.NS");
        early.setQuantity(BigDecimal.ONE);
        early.setPurchasePrice(new BigDecimal("100.00"));
        early.setPurchaseDate(LocalDate.now().minusDays(20));
        early.setCurrentPrice(new BigDecimal("100.00"));

        PortfolioItem later = new PortfolioItem();
        later.setType(AssetType.STOCK);
        later.setSymbolOrName("INFY.NS");
        later.setQuantity(BigDecimal.ONE);
        later.setPurchasePrice(new BigDecimal("50.00"));
        later.setPurchaseDate(LocalDate.now().minusDays(5));
        later.setCurrentPrice(new BigDecimal("50.00"));

        when(repository.findAll()).thenReturn(List.of(early, later));

        PortfolioPerformanceResponse response = service.getPerformance("ALL");

        var beforeLaterPurchase = response.getPoints().stream()
                .filter(p -> p.getDate().equals(later.getPurchaseDate().minusDays(1)))
                .findFirst()
                .orElseThrow();
        // Only "early" should be counted the day before "later" was purchased.
        assertThat(beforeLaterPurchase.getTotalValue()).isEqualByComparingTo("100.00");

        var onOrAfterLaterPurchase = response.getPoints().stream()
                .filter(p -> p.getDate().equals(later.getPurchaseDate()))
                .findFirst()
                .orElseThrow();
        assertThat(onOrAfterLaterPurchase.getTotalValue()).isEqualByComparingTo("150.00");
    }

    @Test
    void getPerformance_interpolatesBetweenPurchaseAndCurrentPrice() {
        LocalDate purchaseDate = LocalDate.now().minusDays(10);
        PortfolioItem stock = new PortfolioItem();
        stock.setType(AssetType.STOCK);
        stock.setSymbolOrName("AAPL");
        stock.setQuantity(BigDecimal.ONE);
        stock.setPurchasePrice(new BigDecimal("100.00"));
        stock.setPurchaseDate(purchaseDate);
        stock.setCurrentPrice(new BigDecimal("200.00"));

        when(repository.findAll()).thenReturn(List.of(stock));

        PortfolioPerformanceResponse response = service.getPerformance("1M");

        // Midpoint (5 days into a 10-day span) should be roughly halfway between 100 and 200.
        var midpoint = response.getPoints().stream()
                .filter(p -> p.getDate().equals(purchaseDate.plusDays(5)))
                .findFirst()
                .orElseThrow();
        assertThat(midpoint.getTotalValue()).isEqualByComparingTo("150.00");
    }
}



