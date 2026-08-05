package com.example.portfolio.service.portfolio;

import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;

import java.math.BigDecimal;

/**
 * Strategy interface for asset-type-specific portfolio item behavior.
 */
public interface PortfolioItemTypeHandler {

    AssetType supportedType();

    void applyCreateDefaults(PortfolioItem item);

    BigDecimal resolveRefreshedPrice(PortfolioItem item);
}

