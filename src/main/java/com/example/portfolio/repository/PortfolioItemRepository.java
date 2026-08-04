package com.example.portfolio.repository;

import com.example.portfolio.model.AssetType;
import java.math.BigDecimal;
import java.util.List;

public interface PortfolioItemRepository {
	List<SummaryItemRow> findItemsForSummary();

	record SummaryItemRow(AssetType type, BigDecimal quantity, BigDecimal purchasePrice, BigDecimal currentPrice) {
	}
}

