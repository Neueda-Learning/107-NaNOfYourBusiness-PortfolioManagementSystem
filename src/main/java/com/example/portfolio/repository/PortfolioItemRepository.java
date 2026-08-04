package com.example.portfolio.repository;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Repository
public class PortfolioItemRepository {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;
    private final PortfolioItemRowMapper rowMapper = new PortfolioItemRowMapper();

    public PortfolioItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.insert = new SimpleJdbcInsert(Objects.requireNonNull(jdbc.getDataSource(), "JdbcTemplate datasource must not be null"))
                .withTableName("portfolio_item")
                .usingGeneratedKeyColumns("id");
    }

    public List<PortfolioItem> findAll() {
        return jdbc.query("SELECT * FROM portfolio_item ORDER BY created_at DESC", rowMapper);
    }

    public List<PortfolioItem> findByType(AssetType type) {
        return jdbc.query(
                "SELECT * FROM portfolio_item WHERE type = ? ORDER BY created_at DESC",
                rowMapper, type.name());
    }

    public Optional<PortfolioItem> findById(Long id) {
        List<PortfolioItem> results = jdbc.query(
                "SELECT * FROM portfolio_item WHERE id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    public PortfolioItem save(PortfolioItem item) {
        LocalDateTime now = LocalDateTime.now();
        Number generatedId = insert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("type", item.getType().name())
                .addValue("symbol_or_name", item.getSymbolOrName())
                .addValue("quantity", item.getQuantity())
                .addValue("purchase_price", item.getPurchasePrice())
                .addValue("purchase_date", item.getPurchaseDate())
                .addValue("current_price", item.getCurrentPrice())
                .addValue("created_at", now)
                .addValue("updated_at", now));

        item.setId(generatedId.longValue());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }

    public PortfolioItem update(PortfolioItem item) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update(
                "UPDATE portfolio_item SET type = ?, symbol_or_name = ?, quantity = ?, purchase_price = ?, " +
                "purchase_date = ?, current_price = ?, updated_at = ? WHERE id = ?",
                item.getType().name(),
                item.getSymbolOrName(),
                item.getQuantity(),
                item.getPurchasePrice(),
                item.getPurchaseDate(),
                item.getCurrentPrice(),
                now,
                item.getId());
        item.setUpdatedAt(now);
        return item;
    }

    public void updateCurrentPrice(Long id, BigDecimal price) {
        jdbc.update("UPDATE portfolio_item SET current_price = ?, updated_at = ? WHERE id = ?",
                price, LocalDateTime.now(), id);
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM portfolio_item WHERE id = ?", id);
    }
}
