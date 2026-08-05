package com.example.portfolio.controller;

import com.example.portfolio.exception.GlobalExceptionHandler;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.service.MutualFundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MutualFundControllerTest {

    private final MutualFundService mutualFundService = mock(MutualFundService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new MutualFundController(mutualFundService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void getSupportedFunds_returnsListOfSupportedFunds() throws Exception {
        List<Object> funds = List.of(
                Map.of("schemeCode", 119551, "schemeName", "HDFC Flexi Cap Fund", "latestNav", 650.25),
                Map.of("schemeCode", 119552, "schemeName", "HDFC Top 100 Fund", "latestNav", 750.50)
        );

        when(mutualFundService.getSupportedFunds()).thenReturn((List) funds);

        mockMvc.perform(get("/api/mutual-funds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].schemeCode").value(119551))
                .andExpect(jsonPath("$[0].schemeName").value("HDFC Flexi Cap Fund"))
                .andExpect(jsonPath("$[1].schemeCode").value(119552));
    }

    @Test
    void getMutualFundDetails_withValidSchemeCode_returnsRawMFAPIResponse() throws Exception {
        Integer schemeCode = 119551;
        Map<String, Object> mfapiResponse = Map.of(
                "meta", Map.of("scheme_name", "HDFC Flexi Cap Fund"),
                "data", List.of(Map.of("nav", "650.25", "date", "2026-08-05"))
        );

        when(mutualFundService.getMutualFundDetails(schemeCode)).thenReturn(mfapiResponse);

        mockMvc.perform(get("/api/mutual-funds/{schemeCode}", schemeCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.scheme_name").value("HDFC Flexi Cap Fund"))
                .andExpect(jsonPath("$.data[0].nav").value("650.25"));
    }

    @Test
    void getMutualFundDetails_withUnsupportedSchemeCode_returns404() throws Exception {
        Integer schemeCode = 999999;

        when(mutualFundService.getMutualFundDetails(schemeCode))
                .thenThrow(new ResourceNotFoundException("Mutual fund is not supported"));

        mockMvc.perform(get("/api/mutual-funds/{schemeCode}", schemeCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Mutual fund is not supported"));
    }

    @Test
    void buyMutualFund_withValidRequest_returnsSuccessMessage() throws Exception {
        Map<String, Object> response = Map.of(
                "message", "Mutual fund purchased successfully",
                "schemeCode", 119551,
                "schemeName", "HDFC Flexi Cap Fund",
                "units", new BigDecimal("15.3778"),
                "nav", new BigDecimal("650.25"),
                "totalAmount", new BigDecimal("10000.00"),
                "portfolioItemId", 1L
        );

        when(mutualFundService.buyMutualFund(any())).thenReturn(response);

        String payload = """
                {
                  "schemeCode": 119551,
                  "amount": 10000.00,
                  "purchaseDate": "2026-08-05"
                }
                """;

        mockMvc.perform(post("/api/mutual-funds/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mutual fund purchased successfully"))
                .andExpect(jsonPath("$.portfolioItemId").value(1));
    }

    @Test
    void buyMutualFund_withInvalidSchemeCode_returns404() throws Exception {
        when(mutualFundService.buyMutualFund(any()))
                .thenThrow(new ResourceNotFoundException("Mutual fund is not supported"));

        String payload = """
                {
                  "schemeCode": 999999,
                  "amount": 10000.00
                }
                """;

        mockMvc.perform(post("/api/mutual-funds/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void buyMutualFund_withInvalidPayload_returns400() throws Exception {
        String payload = """
                {
                  "schemeCode": -1,
                  "amount": -5000.00
                }
                """;

        mockMvc.perform(post("/api/mutual-funds/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void sellMutualFund_withValidRequest_returnsSuccessMessage() throws Exception {
        Map<String, Object> response = Map.of(
                "message", "Mutual fund units sold successfully",
                "portfolioItemId", 1L,
                "unitsSold", new BigDecimal("7.6899"),
                "nav", new BigDecimal("650.25"),
                "totalAmount", new BigDecimal("5000.00"),
                "remainingUnits", new BigDecimal("12.3101")
        );

        when(mutualFundService.sellMutualFund(any())).thenReturn(response);

        String payload = """
                {
                  "portfolioItemId": 1,
                  "amount": 5000.00
                }
                """;

        mockMvc.perform(post("/api/mutual-funds/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mutual fund units sold successfully"))
                .andExpect(jsonPath("$.portfolioItemId").value(1));
    }

    @Test
    void sellMutualFund_withInsufficientUnits_returns400() throws Exception {
        when(mutualFundService.sellMutualFund(any()))
                .thenThrow(new IllegalArgumentException("Not enough units available to sell"));

        String payload = """
                {
                  "portfolioItemId": 1,
                  "amount": 100000.00
                }
                """;

        mockMvc.perform(post("/api/mutual-funds/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void sellMutualFund_withNonExistentPortfolioItem_returns404() throws Exception {
        when(mutualFundService.sellMutualFund(any()))
                .thenThrow(new ResourceNotFoundException("Portfolio item not found with id: 999"));

        String payload = """
                {
                  "portfolioItemId": 999,
                  "amount": 5000.00
                }
                """;

        mockMvc.perform(post("/api/mutual-funds/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}

