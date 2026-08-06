package com.example.portfolio.repository;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database (H2, in-memory) tests for {@link PortfolioItemRepository}.
 * Uses {@code @JdbcTest} so an embedded datasource + {@code schema.sql} are
 * auto-configured, and each test runs in its own rolled-back transaction.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PortfolioItemRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PortfolioItemRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PortfolioItemRepository(jdbcTemplate);
    }

    private PortfolioItem newItem(AssetType type, String symbol, double qty, double price) {
        PortfolioItem item = new PortfolioItem();
        item.setType(type);
        item.setSymbolOrName(symbol);
        item.setQuantity(BigDecimal.valueOf(qty));
        item.setPurchasePrice(BigDecimal.valueOf(price));
        item.setPurchaseDate(LocalDate.of(2026, 1, 10));
        item.setCurrentPrice(BigDecimal.valueOf(price));
        return item;
    }

    @Test
    void save_generatesIdAndTimestamps() {
        PortfolioItem saved = repository.save(newItem(AssetType.STOCK, "AAPL", 10, 150));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_returnsSavedItem() {
        PortfolioItem saved = repository.save(newItem(AssetType.STOCK, "MSFT", 5, 300));

        Optional<PortfolioItem> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getSymbolOrName()).isEqualTo("MSFT");
        assertThat(found.get().getType()).isEqualTo(AssetType.STOCK);
        assertThat(found.get().getQuantity()).isEqualByComparingTo("5");
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        assertThat(repository.findById(987654321L)).isEmpty();
    }

    @Test
    void findAll_returnsAllSavedItems() {
        repository.save(newItem(AssetType.STOCK, "AAPL", 1, 100));
        repository.save(newItem(AssetType.MUTUAL_FUND, "HDFC Flexi Cap Fund", 2, 200));

        List<PortfolioItem> all = repository.findAll();

        assertThat(all).extracting(PortfolioItem::getSymbolOrName)
                .contains("AAPL", "HDFC Flexi Cap Fund");
    }

    @Test
    void findByType_filtersByAssetType() {
        repository.save(newItem(AssetType.STOCK, "AAPL", 1, 100));
        repository.save(newItem(AssetType.BOND, "GOVT-BOND", 1, 1000));

        List<PortfolioItem> stocks = repository.findByType(AssetType.STOCK);
        List<PortfolioItem> bonds = repository.findByType(AssetType.BOND);

        assertThat(stocks).extracting(PortfolioItem::getSymbolOrName).contains("AAPL");
        assertThat(stocks).extracting(PortfolioItem::getType).containsOnly(AssetType.STOCK);
        assertThat(bonds).extracting(PortfolioItem::getSymbolOrName).contains("GOVT-BOND");
    }

    @Test
    void update_changesStoredFields() {
        PortfolioItem saved = repository.save(newItem(AssetType.STOCK, "AAPL", 10, 100));
        saved.setQuantity(BigDecimal.valueOf(20));
        saved.setCurrentPrice(BigDecimal.valueOf(150));
        saved.setSymbolOrName("AAPL-UPDATED");

        PortfolioItem updated = repository.update(saved);

        assertThat(updated.getUpdatedAt()).isNotNull();
        Optional<PortfolioItem> reloaded = repository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getQuantity()).isEqualByComparingTo("20");
        assertThat(reloaded.get().getCurrentPrice()).isEqualByComparingTo("150");
        assertThat(reloaded.get().getSymbolOrName()).isEqualTo("AAPL-UPDATED");
    }

    @Test
    void updateCurrentPrice_updatesOnlyPriceField() {
        PortfolioItem saved = repository.save(newItem(AssetType.STOCK, "AAPL", 10, 100));

        repository.updateCurrentPrice(saved.getId(), BigDecimal.valueOf(175.5));

        Optional<PortfolioItem> reloaded = repository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getCurrentPrice()).isEqualByComparingTo("175.5");
        assertThat(reloaded.get().getQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void updateHoldingAfterTrade_updatesQuantityPriceAndCurrentPrice() {
        PortfolioItem saved = repository.save(newItem(AssetType.STOCK, "AAPL", 10, 100));
        LocalDateTime updatedAt = LocalDateTime.now();

        repository.updateHoldingAfterTrade(saved.getId(), BigDecimal.valueOf(15),
                BigDecimal.valueOf(120), BigDecimal.valueOf(130), updatedAt);

        Optional<PortfolioItem> reloaded = repository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getQuantity()).isEqualByComparingTo("15");
        assertThat(reloaded.get().getPurchasePrice()).isEqualByComparingTo("120");
        assertThat(reloaded.get().getCurrentPrice()).isEqualByComparingTo("130");
    }

    @Test
    void deleteById_removesRow() {
        PortfolioItem saved = repository.save(newItem(AssetType.STOCK, "AAPL", 10, 100));

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteById_withUnknownId_isNoOp() {
        // Should not throw even though no row matches.
        repository.deleteById(123456789L);
    }
}


