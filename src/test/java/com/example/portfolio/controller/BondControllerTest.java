package com.example.portfolio.controller;

import com.example.portfolio.exception.BondRedemptionException;
import com.example.portfolio.exception.GlobalExceptionHandler;
import com.example.portfolio.service.BondService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BondControllerTest {

    private final BondService bondService = mock(BondService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BondController(bondService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void getBondCatalog_routesAllPathToCatalogHandler() throws Exception {
        when(bondService.getBondCatalog()).thenReturn(List.of(bondResponse(10L, "GSEC-2033", "ACTIVE")));

        mockMvc.perform(get("/api/v1/bonds/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].symbol").value("GSEC-2033"));

        verify(bondService).getBondCatalog();
        verify(bondService, never()).getBondDetails(anyLong());
    }

    @Test
    void getBondDetails_routesNumericPathToDetailsHandler() throws Exception {
        when(bondService.getBondDetails(1L)).thenReturn(bondResponse(1L, "SDL-2031", "ACTIVE"));

        mockMvc.perform(get("/api/v1/bonds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.symbol").value("SDL-2031"));

        verify(bondService).getBondDetails(1L);
    }

    @Test
    void redeemBond_whenNotMatured_returnsConflictWithSpecificErrorCode() throws Exception {
        when(bondService.redeemBond(any()))
                .thenThrow(new BondRedemptionException("BOND_NOT_MATURED", "Bond has not yet matured. Maturity date: 2033-01-01"));

        mockMvc.perform(post("/api/v1/bonds/redeem")
                        .contentType("application/json")
                        .content("{\"id\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BOND_NOT_MATURED"))
                .andExpect(jsonPath("$.message").value("Bond has not yet matured. Maturity date: 2033-01-01"));
    }

    @Test
    void redeemBond_whenMatured_returnsOk() throws Exception {
        when(bondService.redeemBond(any())).thenReturn(bondResponse(3L, "NHAI-2025", "REDEEMED"));

        mockMvc.perform(post("/api/v1/bonds/redeem")
                        .contentType("application/json")
                        .content("{\"id\":3}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.symbol").value("NHAI-2025"))
                .andExpect(jsonPath("$.status").value("REDEEMED"));
    }

    private static BondService.BondResponse bondResponse(Long id, String symbol, String status) {
        return new BondService.BondResponse(
                id,
                symbol,
                new BigDecimal("5.0000"),
                new BigDecimal("1000.0000"),
                LocalDate.of(2026, 1, 10),
                new BigDecimal("1005.0000"),
                "RBI",
                new BigDecimal("1000.0000"),
                new BigDecimal("7.5000"),
                "ANNUAL",
                LocalDate.of(2033, 1, 1),
                "AAA",
                new BigDecimal("7.1000"),
                status,
                null,
                null,
                new BigDecimal("5025.0000"),
                new BigDecimal("25.0000"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}

