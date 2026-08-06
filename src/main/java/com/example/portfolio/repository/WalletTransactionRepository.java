package com.example.portfolio.repository;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.WalletTransactionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class WalletTransactionRepository {

    private static final Logger log = LoggerFactory.getLogger(WalletTransactionRepository.class);

    private final SimpleJdbcInsert insert;
    private final JdbcTemplate jdbcTemplate;

    public WalletTransactionRepository(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "DataSource must not be null");
        this.insert = new SimpleJdbcInsert(dataSource)
                .withTableName("wallet_transaction")
                .usingGeneratedKeyColumns("id");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void saveTransaction(Long userId,
                                WalletTransactionType type,
                                BigDecimal amount,
                                BigDecimal balanceAfter,
                                AssetType assetType,
                                Long portfolioItemId,
                                String symbolOrName,
                                LocalDateTime createdAt) {
        insert.execute(new MapSqlParameterSource()
                .addValue("user_data_id", userId)
                .addValue("transaction_type", type.name())
                .addValue("amount", amount)
                .addValue("balance_after", balanceAfter)
                .addValue("asset_type", assetType != null ? assetType.name() : null)
                .addValue("portfolio_item_id", portfolioItemId)
                .addValue("symbol_or_name", symbolOrName)
                .addValue("created_at", createdAt));
        log.debug("Recorded wallet transaction: userId={}, type={}, amount={}, balanceAfter={}",
                userId, type, amount, balanceAfter);
    }

    public List<WalletTransactionRecord> findByUserId(Long userId) {
        String sql = """
                SELECT id, transaction_type, amount, balance_after, asset_type, portfolio_item_id, symbol_or_name, created_at
                FROM wallet_transaction
                WHERE user_data_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new WalletTransactionRecord(
                        rs.getLong("id"),
                        WalletTransactionType.valueOf(rs.getString("transaction_type")),
                        rs.getBigDecimal("amount"),
                        rs.getBigDecimal("balance_after"),
                        rs.getString("asset_type") != null ? AssetType.valueOf(rs.getString("asset_type")) : null,
                        rs.getObject("portfolio_item_id", Long.class),
                        rs.getString("symbol_or_name"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                userId);
    }

    public record WalletTransactionRecord(Long id,
                                          WalletTransactionType type,
                                          BigDecimal amount,
                                          BigDecimal balanceAfter,
                                          AssetType assetType,
                                          Long portfolioItemId,
                                          String symbolOrName,
                                          LocalDateTime createdAt) {
    }
}

