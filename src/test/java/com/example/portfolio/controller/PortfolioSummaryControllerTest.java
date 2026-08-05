package com.example.portfolio.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.portfolio.dto.PortfolioSummaryResponse;
import com.example.portfolio.model.AssetType;
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

	@InjectMocks
	private PortfolioSummaryController portfolioSummaryController;

	@Test
	void getPortfolioSummary_shouldReturnDashboardSummary() {
		PortfolioSummaryResponse response = new PortfolioSummaryResponse(
			new BigDecimal("45230.75"),
			new BigDecimal("41000.00"),
			new BigDecimal("4230.75"),
			new BigDecimal("10.32"),
			14,
			List.of(
				new PortfolioSummaryResponse.AllocationByType(
					AssetType.STOCK,
					new BigDecimal("30250.00"),
					new BigDecimal("66.9")
				)
			)
		);

		when(portfolioSummaryService.getPortfolioSummary()).thenReturn(response);

		ResponseEntity<PortfolioSummaryResponse> result = portfolioSummaryController.getPortfolioSummary();

		assertEquals(200, result.getStatusCode().value());
		assertEquals(14, result.getBody().getItemCount());
		assertEquals(AssetType.STOCK, result.getBody().getAllocationByType().get(0).getType());
		verify(portfolioSummaryService).getPortfolioSummary();
	}
}

