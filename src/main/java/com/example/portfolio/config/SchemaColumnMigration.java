package com.example.portfolio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Adds any {@code portfolio_item} columns that {@code schema.sql} defines but that are
 * missing from a database created by an earlier (smaller) version of the schema.
 *
 * <p>{@code schema.sql} only runs {@code CREATE TABLE IF NOT EXISTS}, which is a no-op once
 * the table already exists — so on a pre-existing database, newly added bond/mutual-fund/
 * stock columns (e.g. {@code issuer}, {@code credit_rating}, {@code status}) would silently
 * stay missing, causing repositories that {@code SELECT *} (e.g. {@code BondRepository}) to
 * fail with "column not found" and surface as {@code 500} errors on endpoints like
 * {@code GET /api/v1/bonds}.
 *
 * <p>Standard MySQL (unlike MariaDB) has no {@code ADD COLUMN IF NOT EXISTS} clause, so this
 * guard can't be expressed as portable SQL in {@code schema.sql}. Instead, this runner
 * inspects {@link DatabaseMetaData} for the actual columns present and issues plain
 * {@code ALTER TABLE ... ADD COLUMN} statements only for the ones that are missing. This
 * works identically against the H2 (dev) and MySQL (prod) datasources.
 */
@Component
@Order(SchemaColumnMigration.RUN_ORDER)
public class SchemaColumnMigration implements CommandLineRunner {

    /** Runs before other {@link CommandLineRunner} beans that may depend on the full schema. */
    static final int RUN_ORDER = 0;

    private static final Logger log = LoggerFactory.getLogger(SchemaColumnMigration.class);

    private static final String TABLE_NAME = "portfolio_item";

    /** Column name -> DDL type/definition, matching schema.sql exactly. */
    private static final Map<String, String> EXPECTED_COLUMNS = new LinkedHashMap<>();

    static {
        EXPECTED_COLUMNS.put("currency", "VARCHAR(10) DEFAULT 'INR'");
        EXPECTED_COLUMNS.put("exchange", "VARCHAR(50)");
        EXPECTED_COLUMNS.put("company_name", "VARCHAR(100)");
        EXPECTED_COLUMNS.put("sector", "VARCHAR(100)");
        EXPECTED_COLUMNS.put("market_cap", "DECIMAL(19,4)");
        EXPECTED_COLUMNS.put("dividend_yield", "DECIMAL(5,2)");
        EXPECTED_COLUMNS.put("issuer", "VARCHAR(100)");
        EXPECTED_COLUMNS.put("face_value", "DECIMAL(19,4)");
        EXPECTED_COLUMNS.put("coupon_rate", "DECIMAL(5,2)");
        EXPECTED_COLUMNS.put("coupon_frequency", "VARCHAR(20)");
        EXPECTED_COLUMNS.put("maturity_date", "DATE");
        EXPECTED_COLUMNS.put("credit_rating", "VARCHAR(10)");
        EXPECTED_COLUMNS.put("yield_rate", "DECIMAL(5,2)");
        EXPECTED_COLUMNS.put("status", "VARCHAR(20) DEFAULT 'ACTIVE'");
        EXPECTED_COLUMNS.put("redemption_date", "DATE");
        EXPECTED_COLUMNS.put("redemption_value", "DECIMAL(19,4)");
        EXPECTED_COLUMNS.put("fund_house", "VARCHAR(100)");
        EXPECTED_COLUMNS.put("category", "VARCHAR(100)");
        EXPECTED_COLUMNS.put("expense_ratio", "DECIMAL(5,2)");
        EXPECTED_COLUMNS.put("risk_level", "VARCHAR(20)");
        EXPECTED_COLUMNS.put("nav", "DECIMAL(19,4)");
    }

    private final JdbcTemplate jdbc;

    public SchemaColumnMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) throws SQLException {
        var existingColumns = jdbc.execute((Connection connection) -> {
            java.util.Set<String> columns = new java.util.HashSet<>();
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, TABLE_NAME, null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                }
            }
            return columns;
        });

        if (existingColumns == null || existingColumns.isEmpty()) {
            // Table doesn't exist yet (shouldn't happen once schema.sql runs), skip.
            return;
        }

        for (Map.Entry<String, String> expected : EXPECTED_COLUMNS.entrySet()) {
            String columnName = expected.getKey();
            if (!existingColumns.contains(columnName)) {
                String ddl = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnName + " " + expected.getValue();
                log.warn("Adding missing column '{}' to '{}' (pre-existing database from an older schema version): {}",
                        columnName, TABLE_NAME, ddl);
                jdbc.execute(ddl);
            }
        }
    }
}


