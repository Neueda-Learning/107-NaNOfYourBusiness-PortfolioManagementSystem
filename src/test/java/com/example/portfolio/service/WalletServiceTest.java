package com.example.portfolio.service;

import com.example.portfolio.exception.InsufficientWalletBalanceException;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.repository.UserDataRepository;
import com.example.portfolio.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserDataRepository userDataRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    private WalletService service;

    @BeforeEach
    void setUp() {
        service = new WalletService(userDataRepository, walletTransactionRepository);
    }

    @Test
    void deposit_increasesBalanceAndRecordsTransaction() {
        when(userDataRepository.getSingleUserId()).thenReturn(1L);
        when(userDataRepository.increaseWalletBalance(eq(1L), eq(new BigDecimal("1000.0000")), any()))
                .thenReturn(new BigDecimal("2500.0000"));

        var response = service.deposit(new BigDecimal("1000"));

        assertThat(response.getBalance()).isEqualByComparingTo("2500.0000");
        verify(walletTransactionRepository).saveTransaction(
                eq(1L),
                eq(com.example.portfolio.model.WalletTransactionType.DEPOSIT),
                eq(new BigDecimal("1000.0000")),
                eq(new BigDecimal("2500.0000")),
                eq(null),
                eq(null),
                eq(null),
                any());
    }

    @Test
    void debitForBuy_whenInsufficient_throwsAndDoesNotRecordTransaction() {
        when(userDataRepository.getSingleUserId()).thenReturn(1L);
        when(userDataRepository.decreaseWalletBalanceIfSufficient(eq(1L), eq(new BigDecimal("500.0000")), any()))
                .thenReturn(false);
        when(userDataRepository.getWalletBalance(1L)).thenReturn(new BigDecimal("100.0000"));

        assertThatThrownBy(() -> service.debitForBuy(new BigDecimal("500"), AssetType.STOCK, 10L, "AAPL"))
                .isInstanceOf(InsufficientWalletBalanceException.class)
                .hasMessageContaining("Insufficient wallet balance");
    }

    @Test
    void getTransactionHistory_mapsRepositoryRecords() {
        when(userDataRepository.getSingleUserId()).thenReturn(1L);
        when(walletTransactionRepository.findByUserId(1L)).thenReturn(List.of(
                new WalletTransactionRepository.WalletTransactionRecord(
                        7L,
                        com.example.portfolio.model.WalletTransactionType.SELL_CREDIT,
                        new BigDecimal("250.0000"),
                        new BigDecimal("1250.0000"),
                        AssetType.STOCK,
                        12L,
                        "MSFT",
                        java.time.LocalDateTime.of(2026, 8, 6, 10, 30))
        ));

        var history = service.getTransactionHistory();

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().getType()).isEqualTo("SELL_CREDIT");
        assertThat(history.getFirst().getAmount()).isEqualByComparingTo("250.0000");
        assertThat(history.getFirst().getAssetType()).isEqualTo("STOCK");
    }
}

