package com.example.portfolio.controller;

import com.example.portfolio.dto.WalletBalanceResponse;
import com.example.portfolio.dto.WalletTransactionResponse;
import com.example.portfolio.exception.GlobalExceptionHandler;
import com.example.portfolio.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletControllerTest {

    private final WalletService walletService = mock(WalletService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WalletController(walletService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void getBalance_returnsWalletBalance() throws Exception {
        when(walletService.getBalance()).thenReturn(
                new WalletBalanceResponse(new BigDecimal("12500.5000"), LocalDateTime.of(2026, 8, 6, 9, 0)));

        mockMvc.perform(get("/api/v1/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(12500.5));
    }

    @Test
    void deposit_withValidPayload_returnsUpdatedBalance() throws Exception {
        when(walletService.deposit(eq(new BigDecimal("5000.00")))).thenReturn(
                new WalletBalanceResponse(new BigDecimal("17500.5000"), LocalDateTime.of(2026, 8, 6, 9, 5)));

        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5000.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(17500.5));
    }

    @Test
    void getTransactionHistory_returnsMostRecentTransactions() throws Exception {
        when(walletService.getTransactionHistory()).thenReturn(List.of(
                new WalletTransactionResponse(
                        11L,
                        "BUY_DEBIT",
                        new BigDecimal("3000.0000"),
                        new BigDecimal("14500.5000"),
                        "STOCK",
                        99L,
                        "AAPL",
                        LocalDateTime.of(2026, 8, 6, 10, 0))
        ));

        mockMvc.perform(get("/api/v1/wallet/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("BUY_DEBIT"))
                .andExpect(jsonPath("$[0].amount").value(3000.0));
    }
}

