package com.example.portfolio.repository;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.model.TradeSide;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class PortfolioTradeRepository {

    private final SimpleJdbcInsert insert;
    private final JdbcTemplate jdbcTemplate;

    public PortfolioTradeRepository(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "DataSource must not be null");
        this.insert = new SimpleJdbcInsert(dataSource)
                .withTableName("portfolio_trade")
                .usingGeneratedKeyColumns("id");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void saveTrade(PortfolioItem item,
                          TradeSide side,
                          BigDecimal quantity,
                          BigDecimal executionPrice,
                          LocalDateTime executedAt) {
        insert.execute(new MapSqlParameterSource()
                .addValue("portfolio_item_id", item.getId())
                .addValue("asset_type", item.getType().name())
                .addValue("symbol_or_name", item.getSymbolOrName())
                .addValue("side", side.name())
                .addValue("quantity", quantity)
                .addValue("execution_price", executionPrice)
                .addValue("executed_at", executedAt)
                .addValue("created_at", executedAt));
    }

    /**
     * Fetch trade history for a specific symbol/name + asset type, most recent first.
     * Used to build per-fund (or per-stock) buy/sell transaction history.
     */
    public List<TradeRecord> findBySymbolAndType(String symbolOrName, AssetType assetType) {
        String sql = """
                SELECT id, side, quantity, execution_price, executed_at
                FROM portfolio_trade
                WHERE symbol_or_name = ? AND asset_type = ?
                ORDER BY executed_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new TradeRecord(
                        rs.getLong("id"),
                        TradeSide.valueOf(rs.getString("side")),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("execution_price"),
                        rs.getTimestamp("executed_at").toLocalDateTime()
                ),
                symbolOrName, assetType.name());
    }

    public record TradeRecord(
            Long id,
            TradeSide side,
            BigDecimal quantity,
            BigDecimal executionPrice,
            LocalDateTime executedAt
    ) {
    }
}
