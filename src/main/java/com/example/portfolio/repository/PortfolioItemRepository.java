package com.example.portfolio.repository;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PortfolioItemRepository {

    private final JdbcTemplate jdbc;
    private final PortfolioItemRowMapper rowMapper = new PortfolioItemRowMapper();

    public PortfolioItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO portfolio_item (type, symbol_or_name, quantity, purchase_price, purchase_date, current_price, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, item.getType().name());
            ps.setString(2, item.getSymbolOrName());
            ps.setBigDecimal(3, item.getQuantity());
            ps.setBigDecimal(4, item.getPurchasePrice());
            ps.setDate(5, Date.valueOf(item.getPurchaseDate()));
            ps.setBigDecimal(6, item.getCurrentPrice());
            ps.setTimestamp(7, Timestamp.valueOf(now));
            ps.setTimestamp(8, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        Number idKey = null;
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            Object rawId = keys.containsKey("id") ? keys.get("id") : keys.get("ID");
            if (rawId instanceof Number numberId) {
                idKey = numberId;
            }
        }
        if (idKey == null) {
            throw new IllegalStateException("Failed to read generated id for portfolio_item insert");
        }
        item.setId(idKey.longValue());
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
                Date.valueOf(item.getPurchaseDate()),
                item.getCurrentPrice(),
                Timestamp.valueOf(now),
                item.getId());
        item.setUpdatedAt(now);
        return item;
    }

    public void updateCurrentPrice(Long id, BigDecimal price) {
        jdbc.update("UPDATE portfolio_item SET current_price = ?, updated_at = ? WHERE id = ?",
                price, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM portfolio_item WHERE id = ?", id);
    }
}
