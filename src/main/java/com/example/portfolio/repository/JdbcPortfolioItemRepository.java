package com.example.portfolio.repository;

import com.example.portfolio.model.AssetType;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPortfolioItemRepository implements PortfolioItemRepository {
	private final JdbcTemplate jdbcTemplate;

	public JdbcPortfolioItemRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<SummaryItemRow> findItemsForSummary() {
		String sql = """
			SELECT type, quantity, purchase_price, current_price
			FROM portfolio_item
			""";

		return jdbcTemplate.query(
			sql,
			(resultSet, rowNum) -> new SummaryItemRow(
				AssetType.valueOf(resultSet.getString("type")),
				resultSet.getBigDecimal("quantity"),
				resultSet.getBigDecimal("purchase_price"),
				resultSet.getBigDecimal("current_price")
			)
		);
	}
}

