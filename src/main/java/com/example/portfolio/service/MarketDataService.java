package com.example.portfolio.service;

import com.example.portfolio.dto.StockQuoteResponse;
import com.example.portfolio.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);

    private final RestClient restClient;
    private final List<String> supportedTickers;

    // Simple in-memory cache
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    private record CachedQuote(BigDecimal price, LocalDateTime fetchedAt) {
        boolean isExpired(Duration ttl) {
            return LocalDateTime.now().isAfter(fetchedAt.plus(ttl));
        }
    }

    public MarketDataService(RestClient marketRestClient,
                             @Value("${market.supported-tickers}") String tickersCsv) {
        this.restClient = marketRestClient;
        this.supportedTickers = Arrays.stream(tickersCsv.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();
    }

    public List<String> getSupportedTickers() {
        return supportedTickers;
    }

    /**
     * Fetch a live quote, using cache if still fresh.
     * Returns empty on any failure — caller decides whether to throw or silently degrade.
     */
    public Optional<StockQuoteResponse> getQuote(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        String symbol = ticker.trim().toUpperCase();

        CachedQuote cached = cache.get(symbol);
        if (cached != null && !cached.isExpired(CACHE_TTL)) {
            log.debug("Cache hit for {}", symbol);
            return Optional.of(new StockQuoteResponse(symbol, cached.price(), "USD", cached.fetchedAt()));
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/cachedPriceData?ticker={ticker}", symbol)
                    .retrieve()
                    .body(Map.class);

            BigDecimal price = extractPrice(symbol, response);
            LocalDateTime now = LocalDateTime.now();
            cache.put(symbol, new CachedQuote(price, now));
            log.info("Fetched live price for {}: {}", symbol, price);
            return Optional.of(new StockQuoteResponse(symbol, price, "USD", now));

        } catch (ExternalApiException e) {
            throw e; // re-throw as-is so controller can return 502
        } catch (Exception e) {
            log.warn("Failed to fetch market data for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Used by PortfolioItemService for silent auto-fetch on create/update.
     */
    public Optional<BigDecimal> fetchPrice(String ticker) {
        try {
            return getQuote(ticker).map(StockQuoteResponse::getPrice);
        } catch (ExternalApiException e) {
            log.warn("Market API unavailable for {}, skipping auto-fetch: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Used by /refresh-price endpoint — throws ExternalApiException (→ 502) if unavailable.
     */
    public BigDecimal fetchPriceOrThrow(String ticker) {
        return getQuote(ticker)
                .map(StockQuoteResponse::getPrice)
                .orElseThrow(() -> new ExternalApiException(
                        "Unable to fetch live price for ticker: " + ticker));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private BigDecimal extractPrice(String symbol, Map<String, Object> response) {
        if (response == null) {
            throw new ExternalApiException("Empty response from market API for " + symbol);
        }
        // The cached API wraps Yahoo-style data; try several common field names
        for (String field : List.of("price", "regularMarketPrice", "currentPrice", "lastPrice", "close")) {
            Object val = response.get(field);
            if (val != null) {
                try {
                    return new BigDecimal(val.toString());
                } catch (NumberFormatException ignore) {
                    // try next field
                }
            }
        }
        // Yahoo Finance nested: {"regularMarketPrice": {"raw": 248.13}}
        Object nested = response.get("regularMarketPrice");
        if (nested instanceof Map<?, ?> nestedMap) {
            Object raw = nestedMap.get("raw");
            if (raw != null) {
                return new BigDecimal(raw.toString());
            }
        }
        throw new ExternalApiException("Could not extract price from market API response for " + symbol);
    }
}
