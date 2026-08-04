package com.example.portfolio.mapper;

import com.example.portfolio.dto.PortfolioItemRequest;
import com.example.portfolio.dto.PortfolioItemResponse;
import com.example.portfolio.model.PortfolioItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PortfolioItemMapper {

    public PortfolioItem toModel(PortfolioItemRequest request) {
        PortfolioItem item = new PortfolioItem();
        item.setType(request.getType());
        item.setSymbolOrName(request.getSymbolOrName().trim().toUpperCase());
        item.setQuantity(request.getQuantity());
        item.setPurchasePrice(request.getPurchasePrice());
        item.setPurchaseDate(request.getPurchaseDate());
        item.setCurrentPrice(request.getCurrentPrice());
        return item;
    }

    public PortfolioItemResponse toResponse(PortfolioItem item) {
        PortfolioItemResponse r = new PortfolioItemResponse();
        r.setId(item.getId());
        r.setType(item.getType());
        r.setSymbolOrName(item.getSymbolOrName());
        r.setQuantity(item.getQuantity());
        r.setPurchasePrice(item.getPurchasePrice());
        r.setPurchaseDate(item.getPurchaseDate());
        r.setCurrentPrice(item.getCurrentPrice());
        r.setCreatedAt(item.getCreatedAt());
        r.setUpdatedAt(item.getUpdatedAt());

        if (item.getCurrentPrice() != null && item.getQuantity() != null && item.getPurchasePrice() != null) {
            BigDecimal currentValue = item.getCurrentPrice().multiply(item.getQuantity())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal cost = item.getPurchasePrice().multiply(item.getQuantity());
            BigDecimal gainLoss = currentValue.subtract(cost).setScale(2, RoundingMode.HALF_UP);
            r.setCurrentValue(currentValue);
            r.setGainLoss(gainLoss);
            if (cost.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal gainLossPercent = gainLoss
                        .divide(cost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                r.setGainLossPercent(gainLossPercent);
            }
        }
        return r;
    }
}
