package com.example.portfolio.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class BondRepository {

    private static final Logger log = LoggerFactory.getLogger(BondRepository.class);

    private static final String BOND_TYPE = "BOND";

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;
    private final RowMapper<BondRecord> rowMapper = new BondRowMapper();

    public BondRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.insert = new SimpleJdbcInsert(Objects.requireNonNull(jdbc.getDataSource(), "JdbcTemplate datasource must not be null"))
                .withTableName("portfolio_item")
                .usingGeneratedKeyColumns("id");
    }

    public List<BondRecord> findAll() {
        String sql = """
                SELECT *
                FROM portfolio_item
                WHERE type = ? AND (status IS NULL OR status = 'ACTIVE')
                ORDER BY created_at DESC
                """;
        return jdbc.query(sql, rowMapper, BOND_TYPE);
    }

    /**
     * Returns every bond row in the database regardless of status (ACTIVE and REDEEMED alike).
     * Used to power the "Available Bonds" catalog so users can reference any bond ever bought.
     */
    public List<BondRecord> findAllAnyStatus() {
        String sql = """
                SELECT *
                FROM portfolio_item
                WHERE type = ?
                ORDER BY created_at DESC
                """;
        return jdbc.query(sql, rowMapper, BOND_TYPE);
    }

    public Optional<BondRecord> findById(Long id) {
        String sql = """
                SELECT *
                FROM portfolio_item
                WHERE id = ? AND type = ? AND (status IS NULL OR status = 'ACTIVE')
                """;
        return jdbc.query(sql, rowMapper, id, BOND_TYPE).stream().findFirst();
    }

    public Optional<BondRecord> findBySymbol(String symbol) {
        String sql = """
                SELECT *
                FROM portfolio_item
                WHERE type = ? AND UPPER(symbol_or_name) = UPPER(?) AND (status IS NULL OR status = 'ACTIVE')
                """;
        return jdbc.query(sql, rowMapper, BOND_TYPE, symbol).stream().findFirst();
    }

    public Optional<BondRecord> findAnyBySymbol(String symbol) {
        String sql = """
                SELECT *
                FROM portfolio_item
                WHERE type = ? AND UPPER(symbol_or_name) = UPPER(?)
                ORDER BY created_at DESC
                """;
        return jdbc.query(sql, rowMapper, BOND_TYPE, symbol).stream().findFirst();
    }

    public List<BondRecord> search(String query,
                                   String issuer,
                                   String creditRating,
                                   LocalDate maturityFrom,
                                   LocalDate maturityTo) {
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM portfolio_item
                WHERE type = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(BOND_TYPE);
        sql.append(" AND (status IS NULL OR status = 'ACTIVE')");

        if (query != null && !query.isBlank()) {
            sql.append(" AND (UPPER(symbol_or_name) LIKE UPPER(?) OR UPPER(COALESCE(issuer, '')) LIKE UPPER(?))");
            String pattern = "%" + query.trim() + "%";
            args.add(pattern);
            args.add(pattern);
        }

        if (issuer != null && !issuer.isBlank()) {
            sql.append(" AND UPPER(COALESCE(issuer, '')) LIKE UPPER(?)");
            args.add("%" + issuer.trim() + "%");
        }

        if (creditRating != null && !creditRating.isBlank()) {
            sql.append(" AND UPPER(COALESCE(credit_rating, '')) = UPPER(?)");
            args.add(creditRating.trim());
        }

        if (maturityFrom != null) {
            sql.append(" AND maturity_date >= ?");
            args.add(Date.valueOf(maturityFrom));
        }

        if (maturityTo != null) {
            sql.append(" AND maturity_date <= ?");
            args.add(Date.valueOf(maturityTo));
        }

        sql.append(" ORDER BY created_at DESC");
        return jdbc.query(sql.toString(), rowMapper, args.toArray());
    }

    public List<BondRecord> findRedeemed() {
        String sql = """
                SELECT *
                FROM portfolio_item
                WHERE type = ? AND status = 'REDEEMED'
                ORDER BY redemption_date DESC
                """;
        return jdbc.query(sql, rowMapper, BOND_TYPE);
    }

    public BondRecord saveNew(BondRecord bond) {
        LocalDateTime now = LocalDateTime.now();
        Number generatedId = insert.executeAndReturnKey(new MapSqlParameterSource()
                .addValue("type", BOND_TYPE)
                .addValue("symbol_or_name", bond.symbol())
                .addValue("quantity", bond.quantity())
                .addValue("purchase_price", bond.purchasePrice())
                .addValue("purchase_date", bond.purchaseDate())
                .addValue("current_price", bond.currentPrice())
                .addValue("issuer", bond.issuer())
                .addValue("face_value", bond.faceValue())
                .addValue("coupon_rate", bond.couponRate())
                .addValue("coupon_frequency", bond.couponFrequency())
                .addValue("maturity_date", bond.maturityDate())
                .addValue("credit_rating", bond.creditRating())
                .addValue("yield_rate", bond.yieldRate())
                .addValue("status", "ACTIVE")
                .addValue("created_at", now)
                .addValue("updated_at", now));

        log.debug("Inserted new bond row: symbol={}, generatedId={}", bond.symbol(), generatedId);
        return bond.withId(generatedId.longValue()).withTimestamps(now, now);
    }

    public BondRecord mergeBuy(BondRecord existing,
                               BigDecimal quantity,
                               BigDecimal purchasePrice,
                               LocalDate purchaseDate,
                               BigDecimal currentPrice,
                               String issuer,
                               BigDecimal faceValue,
                               BigDecimal couponRate,
                               String couponFrequency,
                               LocalDate maturityDate,
                               String creditRating,
                               BigDecimal yieldRate) {
        BigDecimal existingCost = existing.quantity().multiply(existing.purchasePrice());
        BigDecimal buyCost = quantity.multiply(purchasePrice);
        BigDecimal mergedQuantity = existing.quantity().add(quantity);
        BigDecimal mergedPrice = existingCost.add(buyCost)
                .divide(mergedQuantity, 4, RoundingMode.HALF_UP);

        BigDecimal mergedCurrentPrice = currentPrice != null ? currentPrice : existing.currentPrice();
        LocalDateTime now = LocalDateTime.now();

        String sql = """
                UPDATE portfolio_item
                SET quantity = ?,
                    purchase_price = ?,
                    purchase_date = ?,
                    current_price = ?,
                    issuer = ?,
                    face_value = ?,
                    coupon_rate = ?,
                    coupon_frequency = ?,
                    maturity_date = ?,
                    credit_rating = ?,
                    yield_rate = ?,
                    updated_at = ?
                WHERE id = ? AND type = ?
                """;

        jdbc.update(sql,
                mergedQuantity,
                mergedPrice,
                purchaseDate,
                mergedCurrentPrice,
                preferIncoming(issuer, existing.issuer()),
                preferIncoming(faceValue, existing.faceValue()),
                preferIncoming(couponRate, existing.couponRate()),
                preferIncoming(couponFrequency, existing.couponFrequency()),
                preferIncoming(maturityDate, existing.maturityDate()),
                preferIncoming(creditRating, existing.creditRating()),
                preferIncoming(yieldRate, existing.yieldRate()),
                now,
                existing.id(),
                BOND_TYPE);

        return existing.withQuantity(mergedQuantity)
                .withPurchasePrice(mergedPrice)
                .withPurchaseDate(purchaseDate)
                .withCurrentPrice(mergedCurrentPrice)
                .withIssuer(preferIncoming(issuer, existing.issuer()))
                .withFaceValue(preferIncoming(faceValue, existing.faceValue()))
                .withCouponRate(preferIncoming(couponRate, existing.couponRate()))
                .withCouponFrequency(preferIncoming(couponFrequency, existing.couponFrequency()))
                .withMaturityDate(preferIncoming(maturityDate, existing.maturityDate()))
                .withCreditRating(preferIncoming(creditRating, existing.creditRating()))
                .withYieldRate(preferIncoming(yieldRate, existing.yieldRate()))
                .withTimestamps(existing.createdAt(), now);
    }

    public BondRecord applyRedeem(BondRecord existing) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        // Redemption value = face value per unit × quantity; fall back to purchase price if face value is absent
        BigDecimal unitValue = existing.faceValue() != null ? existing.faceValue() : existing.purchasePrice();
        BigDecimal redemptionValue = unitValue.multiply(existing.quantity());

        String sql = """
                UPDATE portfolio_item
                SET status = 'REDEEMED',
                    redemption_date = ?,
                    redemption_value = ?,
                    updated_at = ?
                WHERE id = ? AND type = ?
                """;

        jdbc.update(sql, Date.valueOf(today), redemptionValue, now, existing.id(), BOND_TYPE);

        log.debug("Applied redemption to bond: id={}, symbol={}, redemptionValue={}",
                existing.id(), existing.symbol(), redemptionValue);
        return existing
                .withStatus("REDEEMED")
                .withRedemptionDate(today)
                .withRedemptionValue(redemptionValue)
                .withTimestamps(existing.createdAt(), now);
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM portfolio_item WHERE id = ? AND type = ?", id, BOND_TYPE);
        log.debug("Deleted bond row: id={}", id);
    }

    private static <T> T preferIncoming(T incoming, T fallback) {
        return incoming != null ? incoming : fallback;
    }

    public record BondRecord(Long id,
                             String symbol,
                             BigDecimal quantity,
                             BigDecimal purchasePrice,
                             LocalDate purchaseDate,
                             BigDecimal currentPrice,
                             String issuer,
                             BigDecimal faceValue,
                             BigDecimal couponRate,
                             String couponFrequency,
                             LocalDate maturityDate,
                             String creditRating,
                             BigDecimal yieldRate,
                             String status,
                             LocalDate redemptionDate,
                             BigDecimal redemptionValue,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {

        public BondRecord withId(Long newId) {
            return new BondRecord(newId, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withQuantity(BigDecimal newQuantity) {
            return new BondRecord(id, symbol, newQuantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withPurchasePrice(BigDecimal newPurchasePrice) {
            return new BondRecord(id, symbol, quantity, newPurchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withPurchaseDate(LocalDate newPurchaseDate) {
            return new BondRecord(id, symbol, quantity, purchasePrice, newPurchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withCurrentPrice(BigDecimal newCurrentPrice) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, newCurrentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withIssuer(String newIssuer) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, newIssuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withFaceValue(BigDecimal newFaceValue) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    newFaceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withCouponRate(BigDecimal newCouponRate) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, newCouponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withCouponFrequency(String newCouponFrequency) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, newCouponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withMaturityDate(LocalDate newMaturityDate) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, newMaturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withCreditRating(String newCreditRating) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, newCreditRating, yieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withYieldRate(BigDecimal newYieldRate) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, newYieldRate, status, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withStatus(String newStatus) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, newStatus, redemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withRedemptionDate(LocalDate newRedemptionDate) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, newRedemptionDate, redemptionValue, createdAt, updatedAt);
        }

        public BondRecord withRedemptionValue(BigDecimal newRedemptionValue) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, newRedemptionValue, createdAt, updatedAt);
        }

        public BondRecord withTimestamps(LocalDateTime newCreatedAt, LocalDateTime newUpdatedAt) {
            return new BondRecord(id, symbol, quantity, purchasePrice, purchaseDate, currentPrice, issuer,
                    faceValue, couponRate, couponFrequency, maturityDate, creditRating, yieldRate, status, redemptionDate, redemptionValue, newCreatedAt, newUpdatedAt);
        }
    }

    private static class BondRowMapper implements RowMapper<BondRecord> {

        @Override
        public BondRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BondRecord(
                    rs.getLong("id"),
                    rs.getString("symbol_or_name"),
                    rs.getBigDecimal("quantity"),
                    rs.getBigDecimal("purchase_price"),
                    rs.getDate("purchase_date").toLocalDate(),
                    rs.getBigDecimal("current_price"),
                    rs.getString("issuer"),
                    rs.getBigDecimal("face_value"),
                    rs.getBigDecimal("coupon_rate"),
                    rs.getString("coupon_frequency"),
                    rs.getDate("maturity_date") != null ? rs.getDate("maturity_date").toLocalDate() : null,
                    rs.getString("credit_rating"),
                    rs.getBigDecimal("yield_rate"),
                    rs.getString("status"),
                    rs.getDate("redemption_date") != null ? rs.getDate("redemption_date").toLocalDate() : null,
                    rs.getBigDecimal("redemption_value"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                    rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
            );
        }
    }
}
