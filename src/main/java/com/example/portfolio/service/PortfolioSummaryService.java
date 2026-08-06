package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioSummaryResponse;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.repository.PortfolioItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortfolioSummaryService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioSummaryService.class);

    private final PortfolioItemRepository repository;

    public PortfolioSummaryService(PortfolioItemRepository repository) {
        this.repository = repository;
    }

    public PortfolioSummaryResponse getSummary() {
        List<PortfolioItem> items = repository.findAll();
        log.debug("Computing portfolio summary for {} holding(s)", items.size());

        BigDecimal totalValue = items.stream()
                .filter(i -> i.getCurrentPrice() != null)
                .map(i -> i.getCurrentPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = items.stream()
                .map(i -> i.getPurchasePrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGainLoss = totalValue.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalGainLossPercent = totalCost.compareTo(BigDecimal.ZERO) != 0
                ? totalGainLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Value per type
        Map<AssetType, BigDecimal> valueByType = Arrays.stream(AssetType.values())
                .collect(Collectors.toMap(t -> t, t -> BigDecimal.ZERO));
        Map<AssetType, Integer> countByType = Arrays.stream(AssetType.values())
                .collect(Collectors.toMap(t -> t, t -> 0));
        for (PortfolioItem item : items) {
            countByType.merge(item.getType(), 1, Integer::sum);
            if (item.getCurrentPrice() != null) {
                BigDecimal v = item.getCurrentPrice().multiply(item.getQuantity());
                valueByType.merge(item.getType(), v, BigDecimal::add);
            }
        }

        List<PortfolioSummaryResponse.AllocationEntry> allocation = Arrays.stream(AssetType.values())
                .map(type -> {
                    BigDecimal value = valueByType.get(type).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal percent = totalValue.compareTo(BigDecimal.ZERO) != 0
                            ? value.divide(totalValue, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return new PortfolioSummaryResponse.AllocationEntry(type.name(), value, percent, countByType.get(type));
                })
                .toList();

        return new PortfolioSummaryResponse(
                totalValue.setScale(2, RoundingMode.HALF_UP),
                totalCost.setScale(2, RoundingMode.HALF_UP),
                totalGainLoss,
                totalGainLossPercent,
                items.size(),
                allocation);
    }
}
