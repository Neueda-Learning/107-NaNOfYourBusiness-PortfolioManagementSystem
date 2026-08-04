package com.example.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.example.portfolio.dto.PortfolioSummaryResponse;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.repository.PortfolioItemRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioSummaryServiceTest {

	@Mock
	private PortfolioItemRepository portfolioItemRepository;

	@InjectMocks
	private PortfolioSummaryService portfolioSummaryService;

	@Test
	void getPortfolioSummary_shouldAggregateTotalsAndAllocation() {
		when(portfolioItemRepository.findItemsForSummary()).thenReturn(
			List.of(
				new PortfolioItemRepository.SummaryItemRow(
					AssetType.STOCK,
					new BigDecimal("10"),
					new BigDecimal("100"),
					new BigDecimal("130")
				),
				new PortfolioItemRepository.SummaryItemRow(
					AssetType.BOND,
					new BigDecimal("5"),
					new BigDecimal("200"),
					null
				)
			)
		);

		PortfolioSummaryResponse response = portfolioSummaryService.getPortfolioSummary();

		assertEquals(new BigDecimal("2300.00"), response.getTotalValue());
		assertEquals(new BigDecimal("2000.00"), response.getTotalCost());
		assertEquals(new BigDecimal("300.00"), response.getTotalGainLoss());
		assertEquals(new BigDecimal("15.00"), response.getTotalGainLossPercent());
		assertEquals(2, response.getItemCount());
		assertEquals(2, response.getAllocationByType().size());
	}

	@Test
	void getPortfolioSummary_shouldReturnZerosForEmptyPortfolio() {
		when(portfolioItemRepository.findItemsForSummary()).thenReturn(List.of());

		PortfolioSummaryResponse response = portfolioSummaryService.getPortfolioSummary();

		assertEquals(new BigDecimal("0.00"), response.getTotalValue());
		assertEquals(new BigDecimal("0.00"), response.getTotalCost());
		assertEquals(new BigDecimal("0.00"), response.getTotalGainLoss());
		assertEquals(new BigDecimal("0.00"), response.getTotalGainLossPercent());
		assertEquals(0, response.getItemCount());
		assertEquals(0, response.getAllocationByType().size());
	}
}

