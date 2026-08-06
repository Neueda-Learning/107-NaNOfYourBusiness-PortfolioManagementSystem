package com.example.portfolio.service;

import com.example.portfolio.dto.PortfolioPerformancePoint;
import com.example.portfolio.dto.PortfolioPerformanceResponse;
import com.example.portfolio.model.PortfolioItem;
import com.example.portfolio.repository.PortfolioItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Computes an approximate portfolio value/cost time series for the dashboard's
 * "Performance Over Time" chart (US-15).
 *
 * <p><b>Approximation model:</b> for each holding, the per-day market price between its
 * {@code purchaseDate} (priced at {@code purchasePrice}) and today (priced at
 * {@code currentPrice}) is linearly interpolated. A holding contributes nothing to any
 * date before its own purchase date. This intentionally avoids extra calls to the stock
 * (Twelve Data) or mutual fund (MFAPI) history APIs on every dashboard load — those
 * providers have limited free-tier request budgets, and hitting them per-holding per-day
 * would conflict with the "controlled price refresh" strategy (US-13/NFR-03). Real daily
 * closes are already used for the point-in-time {@code currentPrice}, so the interpolation
 * only smooths the *path* between purchase and now, not the endpoints.
 *
 * <p>Swap-in point for a future, more accurate implementation: replace
 * {@link #priceOnDate} with lookups against {@code MarketDataService#getStockHistory}
 * and {@code MutualFundService#getMutualFundHistory} once a caching/rate-limit strategy
 * for bulk historical reads is in place.
 */
@Service
public class PortfolioPerformanceService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioPerformanceService.class);

    /** Long ranges are sampled weekly to keep the response payload and chart small. */
    private static final int WEEKLY_SAMPLE_THRESHOLD_DAYS = 180;

    private final PortfolioItemRepository repository;

    public PortfolioPerformanceService(PortfolioItemRepository repository) {
        this.repository = repository;
    }

    public PortfolioPerformanceResponse getPerformance(String range) {
        String normalizedRange = normalizeRange(range);
        List<PortfolioItem> items = repository.findAll();

        if (items.isEmpty()) {
            log.debug("No portfolio items found; returning empty performance series for range={}", normalizedRange);
            return new PortfolioPerformanceResponse(normalizedRange, List.of());
        }

        LocalDate today = LocalDate.now();
        LocalDate earliestPurchase = items.stream()
                .map(PortfolioItem::getPurchaseDate)
                .min(Comparator.naturalOrder())
                .orElse(today);

        LocalDate start = resolveStart(normalizedRange, today, earliestPurchase);
        long totalDays = ChronoUnit.DAYS.between(start, today);
        int step = totalDays > WEEKLY_SAMPLE_THRESHOLD_DAYS ? 7 : 1;

        List<PortfolioPerformancePoint> points = new ArrayList<>();
        for (LocalDate d = start; d.isBefore(today); d = d.plusDays(step)) {
            points.add(computePoint(items, d, today));
        }
        // Always include an exact, non-interpolated point for "today".
        points.add(computePoint(items, today, today));

        log.debug("Computed portfolio performance series: range={}, points={}, holdings={}",
                normalizedRange, points.size(), items.size());
        return new PortfolioPerformanceResponse(normalizedRange, points);
    }

    private PortfolioPerformancePoint computePoint(List<PortfolioItem> items, LocalDate date, LocalDate today) {
        BigDecimal value = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            if (item.getPurchaseDate().isAfter(date)) {
                continue; // not yet held as of this date
            }
            BigDecimal price = priceOnDate(item, date, today);
            value = value.add(item.getQuantity().multiply(price));
            cost = cost.add(item.getQuantity().multiply(item.getPurchasePrice()));
        }

        return new PortfolioPerformancePoint(
                date,
                value.setScale(2, RoundingMode.HALF_UP),
                cost.setScale(2, RoundingMode.HALF_UP));
    }

    /** Linearly interpolate a holding's price between its purchase date and today. See class docs. */
    private BigDecimal priceOnDate(PortfolioItem item, LocalDate date, LocalDate today) {
        BigDecimal currentPrice = item.getCurrentPrice() != null ? item.getCurrentPrice() : item.getPurchasePrice();
        LocalDate purchaseDate = item.getPurchaseDate();

        if (!date.isAfter(purchaseDate)) {
            return item.getPurchasePrice();
        }
        if (!date.isBefore(today)) {
            return currentPrice;
        }

        long totalSpanDays = ChronoUnit.DAYS.between(purchaseDate, today);
        if (totalSpanDays <= 0) {
            return currentPrice;
        }

        long elapsedDays = ChronoUnit.DAYS.between(purchaseDate, date);
        BigDecimal fraction = BigDecimal.valueOf(elapsedDays)
                .divide(BigDecimal.valueOf(totalSpanDays), 8, RoundingMode.HALF_UP);
        BigDecimal delta = currentPrice.subtract(item.getPurchasePrice());
        return item.getPurchasePrice().add(delta.multiply(fraction));
    }

    private LocalDate resolveStart(String range, LocalDate today, LocalDate earliestPurchase) {
        LocalDate candidate = switch (range) {
            case "1M" -> today.minusMonths(1);
            case "3M" -> today.minusMonths(3);
            case "6M" -> today.minusMonths(6);
            case "1Y" -> today.minusYears(1);
            default -> earliestPurchase;
        };
        // Never render a flat/empty period before the very first holding existed.
        return candidate.isBefore(earliestPurchase) ? earliestPurchase : candidate;
    }

    private String normalizeRange(String range) {
        if (range == null || range.isBlank()) {
            return "ALL";
        }
        String upper = range.trim().toUpperCase();
        return switch (upper) {
            case "1M", "3M", "6M", "1Y", "ALL" -> upper;
            default -> "ALL";
        };
    }
}

