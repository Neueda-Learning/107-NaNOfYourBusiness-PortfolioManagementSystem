package com.example.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioSummaryResponse {
    private BigDecimal totalValue;
    private BigDecimal totalCost;
    private BigDecimal totalGainLoss;
    private BigDecimal totalGainLossPercent;
    private int itemCount;
    private List<AllocationEntry> allocationByType;

    public static class AllocationEntry {
        private String type;
        private BigDecimal value;
        private BigDecimal percent;
        private int count;

        public AllocationEntry() {
        }

        public AllocationEntry(String type, BigDecimal value, BigDecimal percent, int count) {
            this.type = type;
            this.value = value;
            this.percent = percent;
            this.count = count;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
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

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public PortfolioSummaryResponse() {
    }

    public PortfolioSummaryResponse(BigDecimal totalValue, BigDecimal totalCost, BigDecimal totalGainLoss,
                                    BigDecimal totalGainLossPercent, int itemCount,
                                    List<AllocationEntry> allocationByType) {
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

    public List<AllocationEntry> getAllocationByType() {
        return allocationByType;
    }

    public void setAllocationByType(List<AllocationEntry> allocationByType) {
        this.allocationByType = allocationByType;
    }
}
