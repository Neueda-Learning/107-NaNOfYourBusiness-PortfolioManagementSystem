package com.example.portfolio.repository;

import com.example.portfolio.repository.BondRepository.BondRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database (H2, in-memory) tests for {@link BondRepository}, covering the
 * bond-specific catalog/search/redeem behavior layered on top of {@code portfolio_item}.
 */
@JdbcTest
class BondRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private BondRepository repository;

    @BeforeEach
    void setUp() {
        repository = new BondRepository(jdbcTemplate);
    }

    private BondRecord newBond(String symbol, String issuer, String creditRating, LocalDate maturityDate) {
        return new BondRecord(
                null,
                symbol,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(1000),
                LocalDate.of(2026, 1, 1),
                BigDecimal.valueOf(1000),
                issuer,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(7.5),
                "ANNUAL",
                maturityDate,
                creditRating,
                BigDecimal.valueOf(6.8),
                null, null, null, null, null
        );
    }

    @Test
    void saveNew_persistsBondWithActiveStatusAndGeneratedId() {
        BondRecord saved = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA",
                LocalDate.of(2036, 1, 1)));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.status()).isEqualTo("ACTIVE");
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.updatedAt()).isNotNull();
    }

    @Test
    void findAll_returnsOnlyActiveBonds() {
        BondRecord bond = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));
        repository.applyRedeem(bond);

        BondRecord activeBond = repository.saveNew(newBond("CORP-BOND", "Acme Corp", "BBB", LocalDate.of(2030, 1, 1)));

        List<BondRecord> active = repository.findAll();

        assertThat(active).extracting(BondRecord::symbol).contains("CORP-BOND");
        assertThat(active).extracting(BondRecord::symbol).doesNotContain("US-T-10Y");
    }

    @Test
    void findAllAnyStatus_includesRedeemedBonds() {
        BondRecord bond = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));
        repository.applyRedeem(bond);

        List<BondRecord> all = repository.findAllAnyStatus();

        assertThat(all).extracting(BondRecord::symbol).contains("US-T-10Y");
    }

    @Test
    void findById_onlyReturnsActiveBond() {
        BondRecord bond = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));

        assertThat(repository.findById(bond.id())).isPresent();

        repository.applyRedeem(bond);
        assertThat(repository.findById(bond.id())).isEmpty();
    }

    @Test
    void findBySymbol_isCaseInsensitiveAndActiveOnly() {
        repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));

        Optional<BondRecord> found = repository.findBySymbol("us-t-10y");

        assertThat(found).isPresent();
        assertThat(found.get().symbol()).isEqualTo("US-T-10Y");
    }

    @Test
    void findAnyBySymbol_findsRedeemedBondsToo() {
        BondRecord bond = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));
        repository.applyRedeem(bond);

        Optional<BondRecord> found = repository.findAnyBySymbol("US-T-10Y");

        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo("REDEEMED");
    }

    @Test
    void search_filtersByQueryIssuerRatingAndMaturityRange() {
        repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));
        repository.saveNew(newBond("CORP-BOND", "Acme Corp", "BBB", LocalDate.of(2030, 6, 1)));

        List<BondRecord> byQuery = repository.search("Treasury", null, null, null, null);
        assertThat(byQuery).extracting(BondRecord::symbol).containsExactly("US-T-10Y");

        List<BondRecord> byIssuer = repository.search(null, "Acme", null, null, null);
        assertThat(byIssuer).extracting(BondRecord::symbol).containsExactly("CORP-BOND");

        List<BondRecord> byRating = repository.search(null, null, "AAA", null, null);
        assertThat(byRating).extracting(BondRecord::symbol).containsExactly("US-T-10Y");

        List<BondRecord> byMaturityRange = repository.search(null, null, null,
                LocalDate.of(2029, 1, 1), LocalDate.of(2031, 1, 1));
        assertThat(byMaturityRange).extracting(BondRecord::symbol).containsExactly("CORP-BOND");
    }

    @Test
    void findRedeemed_returnsOnlyRedeemedBonds() {
        BondRecord bond = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));
        repository.saveNew(newBond("CORP-BOND", "Acme Corp", "BBB", LocalDate.of(2030, 1, 1)));
        repository.applyRedeem(bond);

        List<BondRecord> redeemed = repository.findRedeemed();

        assertThat(redeemed).extracting(BondRecord::symbol).containsExactly("US-T-10Y");
    }

    @Test
    void mergeBuy_computesWeightedAveragePurchasePriceAndSumsQuantity() {
        BondRecord existing = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));
        // existing: 10 units @ 1000 = 10000 cost

        BondRecord merged = repository.mergeBuy(existing,
                BigDecimal.valueOf(10), BigDecimal.valueOf(1100), LocalDate.of(2026, 6, 1),
                BigDecimal.valueOf(1100), "US Treasury", BigDecimal.valueOf(1000),
                BigDecimal.valueOf(7.5), "ANNUAL", LocalDate.of(2036, 1, 1), "AAA", BigDecimal.valueOf(6.8));

        // (10000 + 11000) / 20 = 1050
        assertThat(merged.quantity()).isEqualByComparingTo("20");
        assertThat(merged.purchasePrice()).isEqualByComparingTo("1050.0000");

        Optional<BondRecord> reloaded = repository.findById(existing.id());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().quantity()).isEqualByComparingTo("20");
    }

    @Test
    void mergeBuy_keepsExistingValuesWhenIncomingFieldsAreNull() {
        BondRecord existing = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));

        BondRecord merged = repository.mergeBuy(existing,
                BigDecimal.valueOf(5), BigDecimal.valueOf(1000), LocalDate.of(2026, 6, 1),
                null, null, null, null, null, null, null, null);

        assertThat(merged.currentPrice()).isEqualByComparingTo(existing.currentPrice());
        assertThat(merged.issuer()).isEqualTo(existing.issuer());
        assertThat(merged.creditRating()).isEqualTo(existing.creditRating());
    }

    @Test
    void applyRedeem_setsStatusRedemptionDateAndValue() {
        BondRecord bond = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));

        BondRecord redeemed = repository.applyRedeem(bond);

        assertThat(redeemed.status()).isEqualTo("REDEEMED");
        assertThat(redeemed.redemptionDate()).isEqualTo(LocalDate.now());
        // faceValue (1000) * quantity (10) = 10000
        assertThat(redeemed.redemptionValue()).isEqualByComparingTo("10000");
    }

    @Test
    void applyRedeem_fallsBackToPurchasePriceWhenFaceValueMissing() {
        BondRecord bond = newBond("NO-FACE-VALUE", "Acme Corp", "BBB", LocalDate.of(2030, 1, 1));
        bond = bond.withFaceValue(null);
        BondRecord saved = repository.saveNew(bond);

        BondRecord redeemed = repository.applyRedeem(saved);

        // purchasePrice (1000) * quantity (10) = 10000
        assertThat(redeemed.redemptionValue()).isEqualByComparingTo("10000");
    }

    @Test
    void deleteById_removesBondRow() {
        BondRecord bond = repository.saveNew(newBond("US-T-10Y", "US Treasury", "AAA", LocalDate.of(2036, 1, 1)));

        repository.deleteById(bond.id());

        assertThat(repository.findAnyBySymbol("US-T-10Y")).isEmpty();
    }
}


