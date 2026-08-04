package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioSummaryResponse;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.repository.PortfolioItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PortfolioSummaryService {
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final PortfolioItemRepository portfolioItemRepository;

	public PortfolioSummaryService(PortfolioItemRepository portfolioItemRepository) {
		this.portfolioItemRepository = portfolioItemRepository;
	}

	public PortfolioSummaryResponse getPortfolioSummary() {
		List<PortfolioItemRepository.SummaryItemRow> items = portfolioItemRepository.findItemsForSummary();

		BigDecimal totalValue = ZERO;
		BigDecimal totalCost = ZERO;
		Map<AssetType, BigDecimal> allocationValues = new EnumMap<>(AssetType.class);

		for (PortfolioItemRepository.SummaryItemRow item : items) {
			BigDecimal itemCost = item.quantity().multiply(item.purchasePrice());
			BigDecimal effectiveCurrentPrice = item.currentPrice() == null ? item.purchasePrice() : item.currentPrice();
			BigDecimal itemValue = item.quantity().multiply(effectiveCurrentPrice);

			totalCost = totalCost.add(itemCost);
			totalValue = totalValue.add(itemValue);
			allocationValues.merge(item.type(), itemValue, BigDecimal::add);
		}

		BigDecimal totalGainLoss = totalValue.subtract(totalCost);
		BigDecimal totalGainLossPercent = calculatePercentage(totalGainLoss, totalCost, 2);

		return new PortfolioSummaryResponse(
			totalValue.setScale(2, RoundingMode.HALF_UP),
			totalCost.setScale(2, RoundingMode.HALF_UP),
			totalGainLoss.setScale(2, RoundingMode.HALF_UP),
			totalGainLossPercent,
			items.size(),
			buildAllocationByType(allocationValues, totalValue)
		);
	}

	private List<PortfolioSummaryResponse.AllocationByType> buildAllocationByType(
		Map<AssetType, BigDecimal> allocationValues,
		BigDecimal totalValue
	) {
		List<PortfolioSummaryResponse.AllocationByType> allocationByType = new ArrayList<>();

		for (Map.Entry<AssetType, BigDecimal> entry : allocationValues.entrySet()) {
			allocationByType.add(
				new PortfolioSummaryResponse.AllocationByType(
					entry.getKey(),
					entry.getValue().setScale(2, RoundingMode.HALF_UP),
					calculatePercentage(entry.getValue(), totalValue, 1)
				)
			);
		}

		return allocationByType;
	}

	private BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator, int scale) {
		if (denominator.compareTo(ZERO) == 0) {
			return ZERO.setScale(scale, RoundingMode.HALF_UP);
		}

		return numerator
			.multiply(ONE_HUNDRED)
			.divide(denominator, scale, RoundingMode.HALF_UP);
	}
}

