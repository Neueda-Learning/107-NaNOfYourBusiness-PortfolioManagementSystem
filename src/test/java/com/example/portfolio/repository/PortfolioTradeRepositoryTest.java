package com.example.portfolio.repository;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.model.TradeSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database (H2, in-memory) tests for {@link PortfolioTradeRepository},
 * covering the shared buy/sell trade-history log used by both stocks and mutual funds.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PortfolioTradeRepositoryTest {

    @Autowired
    private DataSource dataSource;

    private PortfolioTradeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PortfolioTradeRepository(dataSource);
    }

    private PortfolioItem item(Long id, AssetType type, String symbol) {
        PortfolioItem item = new PortfolioItem();
        item.setId(id);
        item.setType(type);
        item.setSymbolOrName(symbol);
        return item;
    }

    @Test
    void saveTrade_persistsBuyAndSellRecords() {
        PortfolioItem stock = item(1L, AssetType.STOCK, "AAPL");
        LocalDateTime now = LocalDateTime.now();

        repository.saveTrade(stock, TradeSide.BUY, BigDecimal.valueOf(10), BigDecimal.valueOf(150), now);
        repository.saveTrade(stock, TradeSide.SELL, BigDecimal.valueOf(4), BigDecimal.valueOf(160), now.plusMinutes(1));

        List<PortfolioTradeRepository.TradeRecord> trades = repository.findBySymbolAndType("AAPL", AssetType.STOCK);

        assertThat(trades).hasSize(2);
        // Most recent first
        assertThat(trades.get(0).side()).isEqualTo(TradeSide.SELL);
        assertThat(trades.get(0).quantity()).isEqualByComparingTo("4");
        assertThat(trades.get(1).side()).isEqualTo(TradeSide.BUY);
        assertThat(trades.get(1).quantity()).isEqualByComparingTo("10");
    }

    @Test
    void findBySymbolAndType_isScopedByAssetType() {
        repository.saveTrade(item(1L, AssetType.STOCK, "SAME-NAME"), TradeSide.BUY,
                BigDecimal.ONE, BigDecimal.TEN, LocalDateTime.now());
        repository.saveTrade(item(2L, AssetType.MUTUAL_FUND, "SAME-NAME"), TradeSide.BUY,
                BigDecimal.TWO, BigDecimal.TEN, LocalDateTime.now());

        List<PortfolioTradeRepository.TradeRecord> stockTrades =
                repository.findBySymbolAndType("SAME-NAME", AssetType.STOCK);
        List<PortfolioTradeRepository.TradeRecord> fundTrades =
                repository.findBySymbolAndType("SAME-NAME", AssetType.MUTUAL_FUND);

        assertThat(stockTrades).hasSize(1);
        assertThat(fundTrades).hasSize(1);
        assertThat(stockTrades.get(0).quantity()).isEqualByComparingTo("1");
        assertThat(fundTrades.get(0).quantity()).isEqualByComparingTo("2");
    }

    @Test
    void findBySymbolAndType_returnsEmptyForUnknownSymbol() {
        assertThat(repository.findBySymbolAndType("NOPE", AssetType.STOCK)).isEmpty();
    }

    @Test
    void constructor_rejectsNullDataSource() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> new PortfolioTradeRepository(null));
    }
}


