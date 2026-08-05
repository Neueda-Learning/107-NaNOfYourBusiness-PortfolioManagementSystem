package com.example.portfolio.repository;

import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.model.TradeSide;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Repository
public class PortfolioTradeRepository {

    private final SimpleJdbcInsert insert;

    public PortfolioTradeRepository(DataSource dataSource) {
        this.insert = new SimpleJdbcInsert(Objects.requireNonNull(dataSource, "DataSource must not be null"))
                .withTableName("portfolio_trade")
                .usingGeneratedKeyColumns("id");
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
}

