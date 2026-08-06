package com.example.portfolio.controller;

import com.example.portfolio.dto.PortfolioItemResponse;
import com.example.portfolio.dto.PortfolioSummaryResponse;
import com.example.portfolio.dto.StockCatalogItemResponse;
import com.example.portfolio.dto.StockQuoteResponse;
import com.example.portfolio.exception.ExternalApiException;
import com.example.portfolio.exception.GlobalExceptionHandler;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.service.MarketDataService;
import com.example.portfolio.service.PortfolioItemService;
import com.example.portfolio.service.PortfolioPerformanceService;
import com.example.portfolio.service.PortfolioSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiControllerTest {

    private final PortfolioItemService portfolioItemService = mock(PortfolioItemService.class);
    private final PortfolioSummaryService portfolioSummaryService = mock(PortfolioSummaryService.class);
    private final PortfolioPerformanceService portfolioPerformanceService = mock(PortfolioPerformanceService.class);
    private final MarketDataService marketDataService = mock(MarketDataService.class);

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new PortfolioItemController(portfolioItemService),
                    new PortfolioSummaryController(portfolioSummaryService, portfolioPerformanceService),
                    new MarketDataController(marketDataService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void getSupportedTickers_returnsConfiguredList() throws Exception {
        when(marketDataService.getSupportedTickers()).thenReturn(List.of("TCS.NS", "INFY.NS"));

        mockMvc.perform(get("/api/v1/market/supported-tickers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("TCS.NS"))
                .andExpect(jsonPath("$[1]").value("INFY.NS"));
    }

    @Test
    void getStockCatalog_returnsSymbolAndCompanyName() throws Exception {
        when(marketDataService.getStockCatalog()).thenReturn(List.of(
                new StockCatalogItemResponse("TCS.NS", "Tata Consultancy Services Ltd", "INR"),
                new StockCatalogItemResponse("RELIANCE.NS", "Reliance Industries Ltd", "INR")
        ));

        mockMvc.perform(get("/api/v1/market/stock-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("TCS.NS"))
                .andExpect(jsonPath("$[0].companyName").value("Tata Consultancy Services Ltd"))
                .andExpect(jsonPath("$[1].currency").value("INR"));
    }

    @Test
    void getQuote_whenUpstreamUnavailable_returnsBadGateway() throws Exception {
        when(marketDataService.getQuote("TCS.NS")).thenThrow(new ExternalApiException("upstream down"));

        mockMvc.perform(get("/api/v1/market/quote").param("ticker", "TCS.NS"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("EXTERNAL_API_ERROR"));
    }

    @Test
    void getQuote_whenTickerMissing_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/market/quote"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createPortfolioItem_returnsCreatedAndLocation() throws Exception {
        PortfolioItemResponse response = new PortfolioItemResponse();
        response.setId(10L);
        response.setType(AssetType.STOCK);
        response.setSymbolOrName("TCS.NS");
        response.setQuantity(BigDecimal.TEN);
        response.setPurchasePrice(new BigDecimal("100.00"));
        response.setPurchaseDate(LocalDate.of(2025, 1, 1));
        response.setCurrentPrice(new BigDecimal("120.00"));
        response.setCurrentValue(new BigDecimal("1200.00"));
        response.setGainLoss(new BigDecimal("200.00"));
        response.setGainLossPercent(new BigDecimal("20.00"));
        response.setCreatedAt(LocalDateTime.of(2026, 8, 4, 8, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 8, 4, 8, 0));

        doAnswer(invocation -> response).when(portfolioItemService).create(any());

        String payload = """
                {
                  \"type\": \"STOCK\",
                  \"symbolOrName\": \"TCS.NS\",
                  \"quantity\": 10,
                  \"purchasePrice\": 100.00,
                  \"purchaseDate\": \"2025-01-01\"
                }
                """;

        mockMvc.perform(post("/api/v1/portfolio-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/portfolio-items/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.symbolOrName").value("TCS.NS"));
    }

    @Test
    void createPortfolioItem_withInvalidPayload_returnsValidationError() throws Exception {
        String payload = """
                {
                  \"type\": \"STOCK\",
                  \"symbolOrName\": \"\",
                  \"quantity\": 0,
                  \"purchasePrice\": -5,
                  \"purchaseDate\": \"2999-01-01\"
                }
                """;

        mockMvc.perform(post("/api/v1/portfolio-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void deletePortfolioItem_returnsNoContent() throws Exception {
        doNothing().when(portfolioItemService).delete(3L);

        mockMvc.perform(delete("/api/v1/portfolio-items/3"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getPortfolioSummary_returnsSummaryPayload() throws Exception {
        PortfolioSummaryResponse response = new PortfolioSummaryResponse(
                new BigDecimal("1690.00"),
                new BigDecimal("1475.00"),
                new BigDecimal("215.00"),
                new BigDecimal("14.58"),
                2,
                List.of(new PortfolioSummaryResponse.AllocationEntry("STOCK", new BigDecimal("1200.00"), new BigDecimal("71.01"), 1))
        );
        when(portfolioSummaryService.getSummary()).thenReturn(response);

        mockMvc.perform(get("/api/v1/portfolio/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(1690.00))
                .andExpect(jsonPath("$.itemCount").value(2));
    }

    @Test
    void getQuote_returnsQuotePayload() throws Exception {
        when(marketDataService.getQuote("TCS.NS"))
                .thenReturn(Optional.of(new StockQuoteResponse("TCS.NS", new BigDecimal("4120.25"), "INR", LocalDateTime.of(2026, 8, 4, 9, 45))));

        mockMvc.perform(get("/api/v1/market/quote").param("ticker", "TCS.NS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("TCS.NS"))
                .andExpect(jsonPath("$.price").value(4120.25))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void buyPortfolioItem_returnsUpdatedHolding() throws Exception {
        PortfolioItemResponse response = new PortfolioItemResponse();
        response.setId(8L);
        response.setType(AssetType.STOCK);
        response.setSymbolOrName("AAPL");
        response.setQuantity(new BigDecimal("12"));
        response.setPurchasePrice(new BigDecimal("101.10"));
        response.setCurrentPrice(new BigDecimal("120.00"));

        when(portfolioItemService.buy(org.mockito.ArgumentMatchers.eq(8L), org.mockito.ArgumentMatchers.eq(new BigDecimal("2"))))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/portfolio-items/8/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbolOrName").value("AAPL"))
                .andExpect(jsonPath("$.quantity").value(12));
    }

    @Test
    void sellPortfolioItem_withInvalidPayload_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/portfolio-items/8/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}

