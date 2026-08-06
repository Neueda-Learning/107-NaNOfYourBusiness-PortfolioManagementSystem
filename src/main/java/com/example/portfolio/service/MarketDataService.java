package com.example.portfolio.service;

import com.example.portfolio.client.TwelveDataClient;
import com.example.portfolio.dto.StockCatalogItemResponse;
import com.example.portfolio.dto.StockHistoryPoint;
import com.example.portfolio.dto.StockHistoryResponse;
import com.example.portfolio.dto.StockQuoteResponse;
import com.example.portfolio.exception.ExternalApiException;
import com.example.portfolio.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final RestClient finnhubRestClient;
    private final TwelveDataClient twelveDataClient;
    private final List<String> supportedTickers;
    private final Set<String> supportedTickerSet;
    private final int batchSize;
    private final List<List<String>> tickerGroups;
    private final Map<String, String> companyNamesBySymbol;

    // Cache is the single source of truth for prices served to the rest of the app.
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    // Last-known-good history per symbol, used purely as a resilience fallback if
    // Twelve Data is temporarily unavailable. This is request-driven (not polled/staggered)
    // since the free tier's 800 credits/day, 8 calls/min headroom is comfortable for
    // on-demand chart opens.
    private final Map<String, List<StockHistoryPoint>> lastGoodHistory = new ConcurrentHashMap<>();

    // Poll health
    private volatile LocalDateTime lastSuccessfulPoll;
    private volatile String lastPollError;
    private final AtomicInteger nextGroupIndex = new AtomicInteger(0);

    record CachedQuote(
            BigDecimal price,
            BigDecimal change,
            BigDecimal percentChange,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            BigDecimal open,
            BigDecimal previousClose,
            String currency,
            LocalDateTime fetchedAt,
            Long sourceEpochSeconds) {
    }

    public MarketDataService(@Qualifier("finnhubRestClient") RestClient finnhubRestClient,
                             TwelveDataClient twelveDataClient,
                             @Value("${market.supported-tickers}") String tickersCsv,
                             @Value("${marketdata.batch-size:8}") int batchSize) {
        this.finnhubRestClient = finnhubRestClient;
        this.twelveDataClient = twelveDataClient;
        this.batchSize = Math.max(1, batchSize);

        this.supportedTickers = Arrays.stream(tickersCsv.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isBlank())
                .toList();

        this.supportedTickerSet = new HashSet<>(this.supportedTickers);
        this.tickerGroups = splitTickerGroups(this.supportedTickers, this.batchSize);
        this.companyNamesBySymbol = buildCompanyNamesBySymbol();
    }

    public List<String> getSupportedTickers() {
        return supportedTickers;
    }

    public List<StockCatalogItemResponse> getStockCatalog() {
        return supportedTickers.stream()
                .map(symbol -> new StockCatalogItemResponse(
                        symbol,
                        companyNamesBySymbol.getOrDefault(symbol, symbol),
                        "USD"))
                .toList();
    }

    /**
     * Cache-only read. Never calls Finnhub directly from request threads.
     */
    public Optional<StockQuoteResponse> getQuote(String ticker) {
        String symbol = normalizeTicker(ticker);
        if (symbol == null || !supportedTickerSet.contains(symbol)) {
            return Optional.empty();
        }

        CachedQuote cached = cache.get(symbol);
        if (cached == null) {
            return Optional.empty();
        }

        return Optional.of(new StockQuoteResponse(symbol, cached.price(), cached.currency(), cached.fetchedAt()));
    }

    public Optional<BigDecimal> fetchPrice(String ticker) {
        try {
            return getQuote(ticker).map(StockQuoteResponse::getPrice);
        } catch (ExternalApiException e) {
            log.warn("Market cache read failed for {}: {}", ticker, e.getMessage());
            return Optional.empty();
        }
    }

    public BigDecimal fetchPriceOrThrow(String ticker) {
        String resolved = normalizeTicker(ticker);
        return getQuote(ticker)
                .map(StockQuoteResponse::getPrice)
                .orElseThrow(() -> new ExternalApiException(
                        "Unable to fetch live price for ticker: " + (resolved == null ? ticker : resolved)));
    }

    @Scheduled(fixedRateString = "${market.poll.interval-ms:10000}")
    public void refreshQuoteCache() {
        if (tickerGroups.isEmpty()) {
            return;
        }

        int groupIndex = Math.floorMod(nextGroupIndex.getAndIncrement(), tickerGroups.size());
        List<String> group = tickerGroups.get(groupIndex);

        int success = 0;
        int failed = 0;
        LocalDateTime touchedAt = LocalDateTime.now();

        for (String symbol : group) {
            try {
                Optional<CachedQuote> quote = fetchFinnhubQuote(symbol, touchedAt);
                if (quote.isPresent()) {
                    cache.put(symbol, quote.get());
                    success++;
                } else {
                    failed++;
                    log.warn("Finnhub quote returned no usable price for {}", symbol);
                }
            } catch (Exception e) {
                failed++;
                log.warn("Finnhub quote refresh failed for {} (stale cache retained): {}", symbol, e.getMessage());
            }
        }

        if (success > 0) {
            lastSuccessfulPoll = touchedAt;
        }
        lastPollError = failed > 0
                ? "group " + groupIndex + " had " + failed + " failures"
                : null;

        log.info("Quote cache tick groupIndex={}/{} symbolsInGroup={} succeeded={} failed={} touchedAt={}",
                groupIndex,
                tickerGroups.size() - 1,
                group.size(),
                success,
                failed,
                touchedAt);
    }

    protected Optional<CachedQuote> fetchFinnhubQuote(String symbol, LocalDateTime touchedAt) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = finnhubRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/quote").queryParam("symbol", symbol).build())
                .retrieve()
                .body(Map.class);

        if (body == null) {
            return Optional.empty();
        }

        BigDecimal currentPrice = parseDecimal(body.get("c"));
        if (currentPrice == null || currentPrice.signum() <= 0) {
            return Optional.empty();
        }

        CachedQuote quote = new CachedQuote(
                currentPrice,
                parseDecimal(body.get("d")),
                parseDecimal(body.get("dp")),
                parseDecimal(body.get("h")),
                parseDecimal(body.get("l")),
                parseDecimal(body.get("o")),
                parseDecimal(body.get("pc")),
                "USD",
                touchedAt,
                parseLong(body.get("t"))
        );

        return Optional.of(quote);
    }

    public Map<String, StockQuoteResponse> getBatchQuotes(Collection<String> tickers) {
        Map<String, StockQuoteResponse> result = new LinkedHashMap<>();
        for (String ticker : tickers) {
            String symbol = normalizeTicker(ticker);
            if (symbol == null) {
                continue;
            }
            CachedQuote cached = cache.get(symbol);
            if (cached != null) {
                result.put(symbol, new StockQuoteResponse(symbol, cached.price(), cached.currency(), cached.fetchedAt()));
            }
        }
        return result;
    }

    public LocalDateTime getLastSuccessfulPoll() {
        return lastSuccessfulPoll;
    }

    public String getLastPollError() {
        return lastPollError;
    }

    /**
     * Get daily price history for a supported ticker via Twelve Data, mapping the
     * requested chart range (1M/3M/6M/1Y/ALL) to a Twelve Data "outputsize" (number
     * of most-recent daily bars to return) so no extra client-side date filtering is
     * needed — Twelve Data already returns just the requested window.
     * This is request-driven (called when a user opens/switches a chart), not polled,
     * so no cache+stagger scheduling is used. On upstream failure, falls back to the
     * last successful response for this symbol (if any), or an empty history —
     * never propagates a 500 up to the controller.
     */
    public StockHistoryResponse getStockHistory(String ticker, String range) {
        String symbol = normalizeTicker(ticker);
        if (symbol == null || !supportedTickerSet.contains(symbol)) {
            throw new ResourceNotFoundException("Stock ticker is not supported: " + ticker);
        }

        String companyName = companyNamesBySymbol.getOrDefault(symbol, symbol);
        String interval = "1day";
        int outputSize = mapRangeToOutputSize(range);

        try {
            List<StockHistoryPoint> points = twelveDataClient.getDailyHistory(symbol, interval, outputSize);
            lastGoodHistory.put(symbol, points);
            log.info("Fetched {} history points for {} (range={}, outputsize={})",
                    points.size(), symbol, range, outputSize);
            return new StockHistoryResponse(symbol, companyName, points);
        } catch (ExternalApiException e) {
            List<StockHistoryPoint> fallback = lastGoodHistory.getOrDefault(symbol, List.of());
            log.warn("Failed to fetch history for {}: {} — falling back to {} cached point(s)",
                    symbol, e.getMessage(), fallback.size());
            return new StockHistoryResponse(symbol, companyName, fallback);
        }
    }

    /**
     * Map a chart range to a Twelve Data outputsize (trading days, roughly).
     * Twelve Data's max outputsize on the free tier is 5000.
     */
    private int mapRangeToOutputSize(String range) {
        return switch (range == null ? "ALL" : range.toUpperCase()) {
            case "1M" -> 22;
            case "3M" -> 66;
            case "6M" -> 132;
            case "1Y" -> 252;
            default -> 1500; // "ALL" — several years of daily bars
        };
    }

    private String normalizeTicker(String rawTicker) {
        if (rawTicker == null || rawTicker.isBlank()) {
            return null;
        }
        return rawTicker.trim().toUpperCase();
    }

    private static List<List<String>> splitTickerGroups(List<String> tickers, int size) {
        List<List<String>> groups = new java.util.ArrayList<>();
        for (int i = 0; i < tickers.size(); i += size) {
            int end = Math.min(i + size, tickers.size());
            groups.add(List.copyOf(tickers.subList(i, end)));
        }
        return groups;
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, String> buildCompanyNamesBySymbol() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("AAPL", "Apple Inc.");
        names.put("MSFT", "Microsoft Corporation");
        names.put("GOOGL", "Alphabet Inc. Class A");
        names.put("AMZN", "Amazon.com, Inc.");
        names.put("META", "Meta Platforms, Inc.");
        names.put("NVDA", "NVIDIA Corporation");
        names.put("TSLA", "Tesla, Inc.");
        names.put("AMD", "Advanced Micro Devices, Inc.");
        names.put("INTC", "Intel Corporation");
        names.put("NFLX", "Netflix, Inc.");
        names.put("JPM", "JPMorgan Chase & Co.");
        names.put("BAC", "Bank of America Corporation");
        names.put("WFC", "Wells Fargo & Company");
        names.put("V", "Visa Inc.");
        names.put("MA", "Mastercard Incorporated");
        names.put("DIS", "The Walt Disney Company");
        names.put("KO", "The Coca-Cola Company");
        names.put("PEP", "PepsiCo, Inc.");
        names.put("PFE", "Pfizer Inc.");
        names.put("JNJ", "Johnson & Johnson");
        names.put("XOM", "Exxon Mobil Corporation");
        names.put("CVX", "Chevron Corporation");
        names.put("T", "AT&T Inc.");
        names.put("CSCO", "Cisco Systems, Inc.");
        return names;
    }
}
