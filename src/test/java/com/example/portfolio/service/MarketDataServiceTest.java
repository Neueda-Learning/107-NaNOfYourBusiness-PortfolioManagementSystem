package com.example.portfolio.service;

import com.example.portfolio.exception.ExternalApiException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketDataServiceTest {

    private final MarketDataService service = new MarketDataService(
            RestClient.builder().baseUrl("https://example.test").build(),
            "AAPL,MSFT,TSLA",
            2
    );

    @Test
    void getSupportedTickers_returnsConfiguredList() {
        assertThat(service.getSupportedTickers()).containsExactly("AAPL", "MSFT", "TSLA");
    }

    @Test
    void getStockCatalog_returnsSymbolCompanyNameAndCurrency() {
        var catalog = service.getStockCatalog();
        assertThat(catalog).hasSize(3);
        assertThat(catalog.getFirst().getSymbol()).isEqualTo("AAPL");
        assertThat(catalog.getFirst().getCurrency()).isEqualTo("USD");
    }

    @Test
    void getQuote_blankTicker_returnsEmpty() {
        assertThat(service.getQuote("   ")).isEmpty();
    }

    @Test
    void getQuote_cacheMiss_returnsEmpty() {
        assertThat(service.getQuote("AAPL")).isEmpty();
    }

    @Test
    void fetchPriceOrThrow_throwsWhenPriceIsMissingInCache() {
        assertThatThrownBy(() -> service.fetchPriceOrThrow("AAPL"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("AAPL");
    }

    @Test
    void fetchPrice_returnsEmptyWhenPriceIsMissingInCache() {
        assertThat(service.fetchPrice("MSFT")).isEmpty();
    }

    @Test
    void getQuote_unsupportedTicker_returnsEmpty() {
        assertThat(service.getQuote("NVDA")).isEmpty();
    }

    @Test
    void refreshQuoteCache_refreshesOneGroupPerTick_roundRobin() {
        List<String> calls = new ArrayList<>();
        TestableMarketDataService batchService = new TestableMarketDataService(
                RestClient.builder().baseUrl("https://example.test").build(),
                calls,
                symbol -> Optional.of(quote(symbol, "100.00"))
        );

        batchService.refreshQuoteCache();
        assertThat(calls).containsExactly("AAPL", "MSFT");
        assertThat(batchService.getQuote("AAPL")).isPresent();
        assertThat(batchService.getQuote("TSLA")).isEmpty();

        batchService.refreshQuoteCache();
        assertThat(calls).containsExactly("AAPL", "MSFT", "TSLA");
        assertThat(batchService.getQuote("TSLA")).isPresent();

        batchService.refreshQuoteCache();
        assertThat(calls).containsExactly("AAPL", "MSFT", "TSLA", "AAPL", "MSFT");
    }

    @Test
    void refreshQuoteCache_keepsStaleValuesWhenOneSymbolFails() {
        AtomicInteger tslaAttempts = new AtomicInteger();
        TestableMarketDataService batchService = new TestableMarketDataService(
                RestClient.builder().baseUrl("https://example.test").build(),
                new ArrayList<>(),
                symbol -> {
                    if ("TSLA".equals(symbol) && tslaAttempts.incrementAndGet() >= 1) {
                        throw new RuntimeException("429 Too Many Requests");
                    }
                    return Optional.of(quote(symbol, "101.25"));
                }
        );

        batchService.refreshQuoteCache(); // AAPL, MSFT
        batchService.refreshQuoteCache(); // TSLA fails

        assertThat(batchService.getQuote("AAPL")).isPresent();
        assertThat(batchService.getQuote("AAPL").orElseThrow().getPrice()).isEqualByComparingTo("101.25");
        assertThat(batchService.getQuote("TSLA")).isEmpty();
        assertThat(batchService.getLastPollError()).isNotNull();
    }

    @Test
    void getBatchQuotes_readsFromCacheOnly() {
        TestableMarketDataService batchService = new TestableMarketDataService(
                RestClient.builder().baseUrl("https://example.test").build(),
                new ArrayList<>(),
                symbol -> Optional.of(quote(symbol, "222.22"))
        );

        batchService.refreshQuoteCache(); // refreshes AAPL + MSFT

        Map<String, ?> quotes = batchService.getBatchQuotes(List.of("AAPL", "MSFT", "TSLA"));
        assertThat(quotes.keySet()).containsExactly("AAPL", "MSFT");
    }

    private static MarketDataService.CachedQuote quote(String symbol, String price) {
        return new MarketDataService.CachedQuote(
                new BigDecimal(price),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(price),
                new BigDecimal(price),
                new BigDecimal(price),
                new BigDecimal(price),
                "USD",
                LocalDateTime.now(),
                null
        );
    }

    private static class TestableMarketDataService extends MarketDataService {
        private final List<String> calls;
        private final SymbolFetcher fetcher;

        TestableMarketDataService(RestClient finnhubRestClient,
                                  List<String> calls,
                                  SymbolFetcher fetcher) {
            super(finnhubRestClient, "AAPL,MSFT,TSLA", 2);
            this.calls = calls;
            this.fetcher = fetcher;
        }

        @Override
        protected Optional<CachedQuote> fetchFinnhubQuote(String symbol, LocalDateTime touchedAt) {
            calls.add(symbol);
            return fetcher.fetch(symbol);
        }
    }

    @FunctionalInterface
    private interface SymbolFetcher {
        Optional<MarketDataService.CachedQuote> fetch(String symbol);
    }
}
