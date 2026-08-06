package com.example.portfolio.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.portfolio.dto.PortfolioPerformanceResponse;
import com.example.portfolio.dto.PortfolioSummaryResponse;
import com.example.portfolio.service.PortfolioPerformanceService;
import com.example.portfolio.service.PortfolioSummaryService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PortfolioSummaryControllerTest {

	@Mock
	private PortfolioSummaryService portfolioSummaryService;

	@Mock
	private PortfolioPerformanceService portfolioPerformanceService;

	@InjectMocks
	private PortfolioSummaryController portfolioSummaryController;

	@Test
	void getSummary_shouldReturnDashboardSummary() {
		PortfolioSummaryResponse response = new PortfolioSummaryResponse(
			new BigDecimal("45230.75"),
			new BigDecimal("41000.00"),
			new BigDecimal("4230.75"),
			new BigDecimal("10.32"),
			14,
			List.of(
				new PortfolioSummaryResponse.AllocationEntry(
					"STOCK",
					new BigDecimal("30250.00"),
					new BigDecimal("66.9"),
					9
				)
			)
		);

		when(portfolioSummaryService.getSummary()).thenReturn(response);

		ResponseEntity<PortfolioSummaryResponse> result = portfolioSummaryController.getSummary();

		assertEquals(200, result.getStatusCode().value());
		assertEquals(14, result.getBody().getItemCount());
		assertEquals("STOCK", result.getBody().getAllocationByType().get(0).getType());
		verify(portfolioSummaryService).getSummary();
	}

	@Test
	void getPerformance_shouldReturnPerformanceSeries() {
		PortfolioPerformanceResponse response = new PortfolioPerformanceResponse("1M", List.of());
		when(portfolioPerformanceService.getPerformance("1M")).thenReturn(response);

		ResponseEntity<PortfolioPerformanceResponse> result = portfolioSummaryController.getPerformance("1M");

		assertEquals(200, result.getStatusCode().value());
		assertEquals("1M", result.getBody().getRange());
		verify(portfolioPerformanceService).getPerformance("1M");
	}
}

