package com.example.portfolio.dto;

import com.example.portfolio.model.AssetType;
import java.math.BigDecimal;
import java.util.List;

public class PortfolioSummaryResponse {
	private BigDecimal totalValue;
	private BigDecimal totalCost;
	private BigDecimal totalGainLoss;
	private BigDecimal totalGainLossPercent;
	private int itemCount;
	private List<AllocationByType> allocationByType;

	public PortfolioSummaryResponse() {
	}

	public PortfolioSummaryResponse(
		BigDecimal totalValue,
		BigDecimal totalCost,
		BigDecimal totalGainLoss,
		BigDecimal totalGainLossPercent,
		int itemCount,
		List<AllocationByType> allocationByType
	) {
		this.totalValue = totalValue;
		this.totalCost = totalCost;
		this.totalGainLoss = totalGainLoss;
		this.totalGainLossPercent = totalGainLossPercent;
		this.itemCount = itemCount;
		this.allocationByType = allocationByType;
	}

	public BigDecimal getTotalValue() {
		return totalValue;
	}

	public void setTotalValue(BigDecimal totalValue) {
		this.totalValue = totalValue;
	}

	public BigDecimal getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(BigDecimal totalCost) {
		this.totalCost = totalCost;
	}

	public BigDecimal getTotalGainLoss() {
		return totalGainLoss;
	}

	public void setTotalGainLoss(BigDecimal totalGainLoss) {
		this.totalGainLoss = totalGainLoss;
	}

	public BigDecimal getTotalGainLossPercent() {
		return totalGainLossPercent;
	}

	public void setTotalGainLossPercent(BigDecimal totalGainLossPercent) {
		this.totalGainLossPercent = totalGainLossPercent;
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public List<AllocationByType> getAllocationByType() {
		return allocationByType;
	}

	public void setAllocationByType(List<AllocationByType> allocationByType) {
		this.allocationByType = allocationByType;
	}

	public static class AllocationByType {
		private AssetType type;
		private BigDecimal value;
		private BigDecimal percent;

		public AllocationByType() {
		}

		public AllocationByType(AssetType type, BigDecimal value, BigDecimal percent) {
			this.type = type;
			this.value = value;
			this.percent = percent;
		}

		public AssetType getType() {
			return type;
		}

		public void setType(AssetType type) {
			this.type = type;
		}

		public BigDecimal getValue() {
			return value;
		}

		public void setValue(BigDecimal value) {
			this.value = value;
		}

		public BigDecimal getPercent() {
			return percent;
		}

		public void setPercent(BigDecimal percent) {
			this.percent = percent;
		}
	}
}

