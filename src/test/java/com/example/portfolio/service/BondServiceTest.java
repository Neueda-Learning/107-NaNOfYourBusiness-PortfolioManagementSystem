package com.example.portfolio.service;

import com.example.portfolio.exception.BondRedemptionException;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.BondRepository;
import com.example.portfolio.repository.BondRepository.BondRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BondServiceTest {

    @Mock
    private BondRepository repository;

    private BondService service;

    @BeforeEach
    void setUp() {
        service = new BondService(repository);
    }

    private BondRecord activeBond(Long id, String symbol, BigDecimal quantity, BigDecimal purchasePrice,
                                  BigDecimal currentPrice, LocalDate maturityDate) {
        return new BondRecord(id, symbol, quantity, purchasePrice, LocalDate.of(2025, 1, 1), currentPrice,
                "US Treasury", BigDecimal.valueOf(1000), BigDecimal.valueOf(7.5), "ANNUAL",
                maturityDate, "AAA", BigDecimal.valueOf(6.8), "ACTIVE", null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getAllBonds_mapsRepositoryRecordsToResponsesWithComputedFields() {
        BondRecord record = activeBond(1L, "US-T-10Y", BigDecimal.TEN, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1100), LocalDate.of(2036, 1, 1));
        when(repository.findAll()).thenReturn(List.of(record));

        List<BondService.BondResponse> result = service.getAllBonds();

        assertThat(result).hasSize(1);
        BondService.BondResponse response = result.get(0);
        assertThat(response.currentValue()).isEqualByComparingTo("11000"); // 10 * 1100
        assertThat(response.gainLoss()).isEqualByComparingTo("1000");      // 11000 - 10000
    }

    @Test
    void getBondCatalog_returnsAllRegardlessOfStatus() {
        when(repository.findAllAnyStatus()).thenReturn(List.of(
                activeBond(1L, "A", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, LocalDate.of(2030, 1, 1))));

        assertThat(service.getBondCatalog()).hasSize(1);
        verify(repository).findAllAnyStatus();
    }

    @Test
    void getBondDetails_returnsMappedResponse() {
        BondRecord record = activeBond(5L, "US-T-10Y", BigDecimal.TEN, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000), LocalDate.of(2036, 1, 1));
        when(repository.findById(5L)).thenReturn(Optional.of(record));

        BondService.BondResponse response = service.getBondDetails(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.symbol()).isEqualTo("US-T-10Y");
    }

    @Test
    void getBondDetails_withUnknownId_throwsResourceNotFoundException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBondDetails(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void getRedeemedBonds_returnsMappedResponses() {
        when(repository.findRedeemed()).thenReturn(List.of(
                activeBond(1L, "A", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, LocalDate.of(2030, 1, 1))));

        assertThat(service.getRedeemedBonds()).hasSize(1);
    }

    @Test
    void searchBonds_withMaturityFromAfterMaturityTo_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.searchBonds(null, null, null,
                LocalDate.of(2030, 1, 1), LocalDate.of(2020, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maturityFrom must be before or equal to maturityTo");
    }

    @Test
    void searchBonds_delegatesToRepositoryAndMapsResults() {
        when(repository.search("t", "issuer", "AAA", null, null)).thenReturn(List.of(
                activeBond(1L, "A", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, LocalDate.of(2030, 1, 1))));

        List<BondService.BondResponse> result = service.searchBonds("t", "issuer", "AAA", null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void buyBond_withNewSymbol_savesNewActiveBond() {
        BondService.BondTradeRequest request = new BondService.BondTradeRequest(
                "us-t-10y", BigDecimal.TEN, BigDecimal.valueOf(1000), LocalDate.of(2026, 1, 1),
                null, "US Treasury", BigDecimal.valueOf(1000), BigDecimal.valueOf(7.5), "ANNUAL",
                LocalDate.of(2036, 1, 1), "AAA", BigDecimal.valueOf(6.8));

        when(repository.findBySymbol("US-T-10Y")).thenReturn(Optional.empty());
        ArgumentCaptor<BondRecord> captor = ArgumentCaptor.forClass(BondRecord.class);
        when(repository.saveNew(captor.capture())).thenAnswer(inv -> {
            BondRecord r = inv.getArgument(0);
            return r.withId(1L).withTimestamps(LocalDateTime.now(), LocalDateTime.now());
        });

        BondService.BondResponse response = service.buyBond(request);

        assertThat(response.symbol()).isEqualTo("US-T-10Y"); // normalized to uppercase
        // currentPrice defaults to purchasePrice when not provided
        assertThat(captor.getValue().currentPrice()).isEqualByComparingTo("1000");
        verify(repository, never()).mergeBuy(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void buyBond_withExistingSymbol_mergesIntoExistingHolding() {
        BondRecord existing = activeBond(1L, "US-T-10Y", BigDecimal.TEN, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000), LocalDate.of(2036, 1, 1));
        BondService.BondTradeRequest request = new BondService.BondTradeRequest(
                "US-T-10Y", BigDecimal.TEN, BigDecimal.valueOf(1100), LocalDate.of(2026, 6, 1),
                BigDecimal.valueOf(1100), null, null, null, null, null, null, null);

        when(repository.findBySymbol("US-T-10Y")).thenReturn(Optional.of(existing));
        when(repository.mergeBuy(eq(existing), eq(BigDecimal.TEN), eq(BigDecimal.valueOf(1100)),
                eq(LocalDate.of(2026, 6, 1)), eq(BigDecimal.valueOf(1100)), eq(null), eq(null),
                eq(null), eq(null), eq(null), eq(null), eq(null)))
                .thenReturn(existing.withQuantity(BigDecimal.valueOf(20)));

        BondService.BondResponse response = service.buyBond(request);

        assertThat(response.quantity()).isEqualByComparingTo("20");
        verify(repository, never()).saveNew(any());
    }

    @Test
    void buyBond_withZeroQuantity_throwsIllegalArgumentException() {
        BondService.BondTradeRequest request = new BondService.BondTradeRequest(
                "US-T-10Y", BigDecimal.ZERO, BigDecimal.valueOf(1000), LocalDate.of(2026, 1, 1),
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.buyBond(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be greater than 0");
    }

    @Test
    void buyBond_withZeroPurchasePrice_throwsIllegalArgumentException() {
        BondService.BondTradeRequest request = new BondService.BondTradeRequest(
                "US-T-10Y", BigDecimal.TEN, BigDecimal.ZERO, LocalDate.of(2026, 1, 1),
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.buyBond(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("purchasePrice must be greater than 0");
    }

    @Test
    void buyBond_withBlankSymbol_throwsIllegalArgumentException() {
        BondService.BondTradeRequest request = new BondService.BondTradeRequest(
                "   ", BigDecimal.TEN, BigDecimal.valueOf(1000), LocalDate.of(2026, 1, 1),
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.buyBond(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("symbol is required");
    }

    @Test
    void redeemBond_withMaturedBond_marksRedeemed() {
        BondRecord existing = activeBond(1L, "US-T-10Y", BigDecimal.TEN, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000), LocalDate.of(2020, 1, 1)); // matured in the past
        when(repository.findAnyBySymbol("US-T-10Y")).thenReturn(Optional.of(existing));
        when(repository.applyRedeem(existing)).thenReturn(existing.withStatus("REDEEMED")
                .withRedemptionDate(LocalDate.now())
                .withRedemptionValue(BigDecimal.valueOf(10000)));

        BondService.BondResponse response = service.redeemBond(new BondService.BondRedeemRequest("us-t-10y"));

        assertThat(response.status()).isEqualTo("REDEEMED");
        assertThat(response.redemptionValue()).isEqualByComparingTo("10000");
    }

    @Test
    void redeemBond_withUnknownSymbol_throwsResourceNotFoundException() {
        when(repository.findAnyBySymbol("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeemBond(new BondService.BondRedeemRequest("unknown")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void redeemBond_whenAlreadyRedeemed_throwsBondRedemptionException() {
        BondRecord redeemed = activeBond(1L, "US-T-10Y", BigDecimal.TEN, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000), LocalDate.of(2020, 1, 1))
                .withStatus("REDEEMED").withRedemptionDate(LocalDate.of(2025, 1, 1));
        when(repository.findAnyBySymbol("US-T-10Y")).thenReturn(Optional.of(redeemed));

        assertThatThrownBy(() -> service.redeemBond(new BondService.BondRedeemRequest("US-T-10Y")))
                .isInstanceOf(BondRedemptionException.class)
                .hasMessageContaining("already redeemed");
    }

    @Test
    void redeemBond_withMissingMaturityDate_throwsBondRedemptionException() {
        BondRecord noMaturity = activeBond(1L, "US-T-10Y", BigDecimal.TEN, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000), null);
        when(repository.findAnyBySymbol("US-T-10Y")).thenReturn(Optional.of(noMaturity));

        assertThatThrownBy(() -> service.redeemBond(new BondService.BondRedeemRequest("US-T-10Y")))
                .isInstanceOf(BondRedemptionException.class)
                .hasMessageContaining("no maturity date");
    }

    @Test
    void redeemBond_beforeMaturity_throwsBondRedemptionException() {
        BondRecord notMatured = activeBond(1L, "US-T-10Y", BigDecimal.TEN, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000), LocalDate.now().plusYears(5));
        when(repository.findAnyBySymbol("US-T-10Y")).thenReturn(Optional.of(notMatured));

        assertThatThrownBy(() -> service.redeemBond(new BondService.BondRedeemRequest("US-T-10Y")))
                .isInstanceOf(BondRedemptionException.class)
                .hasMessageContaining("not yet matured");
    }
}

