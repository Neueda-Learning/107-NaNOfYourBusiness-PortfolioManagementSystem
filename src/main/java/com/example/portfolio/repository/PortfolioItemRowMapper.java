package com.example.portfolio.repository;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PortfolioItemRowMapper implements RowMapper<PortfolioItem> {

    @Override
    public PortfolioItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        PortfolioItem item = new PortfolioItem();
        item.setId(rs.getLong("id"));
        item.setType(AssetType.valueOf(rs.getString("type")));
        item.setSymbolOrName(rs.getString("symbol_or_name"));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        item.setPurchaseDate(rs.getDate("purchase_date").toLocalDate());
        item.setCurrentPrice(rs.getBigDecimal("current_price"));
        if (rs.getTimestamp("created_at") != null) {
            item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            item.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return item;
    }
}
