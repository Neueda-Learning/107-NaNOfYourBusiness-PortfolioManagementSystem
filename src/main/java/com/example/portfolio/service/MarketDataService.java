package com.example.portfolio.service;

import com.example.portfolio.dto.StockCatalogItemResponse;
import com.example.portfolio.dto.StockQuoteResponse;
import com.example.portfolio.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);
    private static final RestClient YAHOO_REST_CLIENT = RestClient.builder()
            .baseUrl("https://query1.finance.yahoo.com")
            .defaultHeader("Accept", "application/json")
            .build();
    private static final RestClient YAHOO_CHART_REST_CLIENT = RestClient.builder()
            .baseUrl("https://query2.finance.yahoo.com")
            .defaultHeader("Accept", "application/json")
            .build();
    private static final RestClient ALPHA_VANTAGE_REST_CLIENT = RestClient.builder()
            .baseUrl("https://www.alphavantage.co")
            .defaultHeader("Accept", "application/json")
            .build();

    private final RestClient restClient;
    private final List<String> supportedTickers;
    private final Set<String> supportedTickerSet;
    private final String alphaVantageApiKey;
    private final boolean yahooLibraryEnabled;
    private final boolean yahooHttpEnabled;
    private final Map<String, String> companyNamesBySymbol;

    // Simple in-memory cache
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    private record CachedQuote(BigDecimal price, String currency, LocalDateTime fetchedAt) {
        boolean isExpired(Duration ttl) {
            return LocalDateTime.now().isAfter(fetchedAt.plus(ttl));
        }
    }

    public MarketDataService(RestClient marketRestClient,
                             @Value("${market.supported-tickers}") String tickersCsv,
                             @Value("${market.alpha-vantage.api-key:}") String alphaVantageApiKey,
                             @Value("${market.yahoo-library.enabled:true}") boolean yahooLibraryEnabled,
                             @Value("${market.yahoo-http.enabled:true}") boolean yahooHttpEnabled) {
        this.restClient = marketRestClient;
        this.alphaVantageApiKey = alphaVantageApiKey == null ? "" : alphaVantageApiKey.trim();
        this.yahooLibraryEnabled = yahooLibraryEnabled;
        this.yahooHttpEnabled = yahooHttpEnabled;
        this.supportedTickers = Arrays.stream(tickersCsv.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();
        this.supportedTickerSet = new HashSet<>(this.supportedTickers);
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
                        defaultCurrencyForSymbol(symbol)))
                .toList();
    }

    /**
     * Fetch a live quote, using cache if still fresh.
     * Returns empty on any failure — caller decides whether to throw or silently degrade.
     */
    public Optional<StockQuoteResponse> getQuote(String ticker) {
        String symbol = normalizeTicker(ticker);
        if (symbol == null) {
            return Optional.empty();
        }
        if (!supportedTickerSet.contains(symbol)) {
            log.warn("Rejected unsupported ticker {}", symbol);
            return Optional.empty();
        }

        CachedQuote cached = cache.get(symbol);
        if (cached != null && !cached.isExpired(CACHE_TTL)) {
            log.debug("Cache hit for {}", symbol);
            return Optional.of(new StockQuoteResponse(symbol, cached.price(), cached.currency(), cached.fetchedAt()));
        }

        // Provider 1: Yahoo Finance Java library
        if (yahooLibraryEnabled) {
            try {
                Optional<StockQuoteResponse> quote = fetchFromYahooFinanceLibrary(symbol);
                if (quote.isPresent()) {
                    return quote;
                }
            } catch (Exception e) {
                log.warn("Yahoo library provider failed for {}: {}", symbol, e.getMessage());
            }
        }

        if (yahooHttpEnabled) {
            // Provider 2: direct Yahoo Finance quote endpoint
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> yahooResponse = YAHOO_REST_CLIENT.get()
                        .uri("/v7/finance/quote?symbols={ticker}", symbol)
                        .retrieve()
                        .body(Map.class);
                BigDecimal price = extractPrice(symbol, yahooResponse);
                String currency = extractCurrency(symbol, yahooResponse);
                return cacheAndBuildQuote(symbol, price, currency);
            } catch (Exception e) {
                log.warn("Yahoo HTTP quote provider failed for {}: {}", symbol, e.getMessage());
            }

            // Provider 3: Yahoo chart endpoint (often works when quote endpoint is rate-limited)
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> chartResponse = YAHOO_CHART_REST_CLIENT.get()
                        .uri("/v8/finance/chart/{ticker}?interval=1d&range=5d", symbol)
                        .retrieve()
                        .body(Map.class);
                BigDecimal price = extractPrice(symbol, chartResponse);
                String currency = extractCurrency(symbol, chartResponse);
                return cacheAndBuildQuote(symbol, price, currency);
            } catch (Exception e) {
                log.warn("Yahoo HTTP chart provider failed for {}: {}", symbol, e.getMessage());
            }
        }

        // Provider 4: configured market API proxy
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/cachedPriceData?ticker={ticker}", symbol)
                    .retrieve()
                    .body(Map.class);
            BigDecimal price = extractPrice(symbol, response);
            String currency = extractCurrency(symbol, response);
            return cacheAndBuildQuote(symbol, price, currency);
        } catch (Exception e) {
            log.warn("Configured market provider failed for {}: {}", symbol, e.getMessage());
        }

        // Provider 5: Alpha Vantage (optional, requires key)
        if (!alphaVantageApiKey.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> alphaResponse = ALPHA_VANTAGE_REST_CLIENT.get()
                        .uri("/query?function=GLOBAL_QUOTE&symbol={ticker}&apikey={apiKey}", symbol, alphaVantageApiKey)
                        .retrieve()
                        .body(Map.class);

                BigDecimal price = extractPrice(symbol, alphaResponse);
                String currency = extractCurrency(symbol, alphaResponse);
                return cacheAndBuildQuote(symbol, price, currency);
            } catch (Exception e) {
                log.warn("Alpha Vantage fallback failed for {}: {}", symbol, e.getMessage());
            }
        }

        return Optional.empty();
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
        String resolved = normalizeTicker(ticker);
        return getQuote(ticker)
                .map(StockQuoteResponse::getPrice)
                .orElseThrow(() -> new ExternalApiException(
                        "Unable to fetch live price for ticker: " + (resolved == null ? ticker : resolved)));
    }
    private String normalizeTicker(String rawTicker) {
        if (rawTicker == null || rawTicker.isBlank()) {
            return null;
        }
        String normalized = rawTicker.trim().toUpperCase();
        if (supportedTickerSet.contains(normalized)) {
            return normalized;
        }
        if (!normalized.contains(".")) {
            String withNseSuffix = normalized + ".NS";
            if (supportedTickerSet.contains(withNseSuffix)) {
                return withNseSuffix;
            }
        }
        return normalized;
    }

    private Optional<StockQuoteResponse> fetchFromYahooFinanceLibrary(String symbol) throws IOException {
        Stock stock = YahooFinance.get(symbol);
        if (stock == null || stock.getQuote() == null) {
            return Optional.empty();
        }

        BigDecimal price = stock.getQuote(true).getPrice();
        if (price == null || price.signum() <= 0) {
            price = stock.getQuote().getPrice();
        }
        if (price == null || price.signum() <= 0) {
            return Optional.empty();
        }

        String currency = stock.getCurrency();
        if (currency == null || currency.isBlank()) {
            currency = defaultCurrencyForSymbol(symbol);
        }
        return cacheAndBuildQuote(symbol, price, currency.trim().toUpperCase());
    }


    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private BigDecimal extractPrice(String symbol, Map<String, Object> response) {
        if (response == null) {
            throw new ExternalApiException("Empty response from market API for " + symbol);
        }

        // Cached API shape: { ticker: "C", price_data: { close: [ ... ] } }
        Object priceDataObj = response.get("price_data");
        if (priceDataObj instanceof Map<?, ?> priceDataMap) {
            Object closeObj = priceDataMap.get("close");
            if (closeObj instanceof List<?> closes && !closes.isEmpty()) {
                Object last = closes.get(closes.size() - 1);
                if (last != null) {
                    try {
                        return new BigDecimal(last.toString());
                    } catch (NumberFormatException ignore) {
                        // continue with other extraction paths
                    }
                }
            }
        }

        // Direct Yahoo response shape: { quoteResponse: { result: [ {...} ] } }
        Object quoteResponse = response.get("quoteResponse");
        if (quoteResponse instanceof Map<?, ?> quoteMap) {
            Object resultObj = quoteMap.get("result");
            if (resultObj instanceof List<?> resultList && !resultList.isEmpty()) {
                Object first = resultList.get(0);
                if (first instanceof Map<?, ?> firstQuote) {
                    BigDecimal fromYahooQuote = tryExtractFromMap(firstQuote);
                    if (fromYahooQuote != null) {
                        return fromYahooQuote;
                    }
                }
            }
        }

        // Yahoo chart shape: { chart: { result: [ { meta: {...}, indicators: { quote: [ { close: [...] } ] } } ] } }
        Object chartObj = response.get("chart");
        if (chartObj instanceof Map<?, ?> chartMap) {
            Object chartResultObj = chartMap.get("result");
            if (chartResultObj instanceof List<?> chartResultList && !chartResultList.isEmpty()) {
                Object firstChartResult = chartResultList.get(0);
                if (firstChartResult instanceof Map<?, ?> chartResultMap) {
                    Object metaObj = chartResultMap.get("meta");
                    if (metaObj instanceof Map<?, ?> metaMap) {
                        BigDecimal metaPrice = tryExtractFromMap(metaMap);
                        if (metaPrice != null) {
                            return metaPrice;
                        }
                    }
                    Object indicatorsObj = chartResultMap.get("indicators");
                    if (indicatorsObj instanceof Map<?, ?> indicatorsMap) {
                        Object quoteObj = indicatorsMap.get("quote");
                        if (quoteObj instanceof List<?> quoteList && !quoteList.isEmpty()) {
                            Object firstQuote = quoteList.get(0);
                            if (firstQuote instanceof Map<?, ?> quoteFieldsMap) {
                                Object closeObj = quoteFieldsMap.get("close");
                                if (closeObj instanceof List<?> closes && !closes.isEmpty()) {
                                    for (int i = closes.size() - 1; i >= 0; i--) {
                                        Object lastValidClose = closes.get(i);
                                        if (lastValidClose != null) {
                                            try {
                                                return new BigDecimal(lastValidClose.toString());
                                            } catch (NumberFormatException ignore) {
                                                // continue searching previous closes
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Wrapped shapes, e.g. { data: {...} } / { body: {...} } from proxy APIs
        for (String container : List.of("data", "body", "result", "quote")) {
            Object nested = response.get(container);
            if (nested instanceof Map<?, ?> nestedMap) {
                BigDecimal fromNested = tryExtractFromMap(nestedMap);
                if (fromNested != null) {
                    return fromNested;
                }
            }
        }

        // Alpha Vantage shape: { "Global Quote": { "05. price": "..." } }
        Object globalQuoteObj = response.get("Global Quote");
        if (globalQuoteObj instanceof Map<?, ?> globalQuoteMap) {
            Object alphaPrice = globalQuoteMap.get("05. price");
            if (alphaPrice != null) {
                try {
                    return new BigDecimal(alphaPrice.toString());
                } catch (NumberFormatException ignore) {
                    // keep trying other patterns
                }
            }
        }

        BigDecimal topLevel = tryExtractFromMap(response);
        if (topLevel != null) {
            return topLevel;
        }

        throw new ExternalApiException("Could not extract price from market API response for " + symbol);
    }

    private BigDecimal tryExtractFromMap(Map<?, ?> source) {
        if (source == null) {
            return null;
        }

        // The cached API wraps Yahoo-style data; try several common field names
        for (String field : List.of("price", "regularMarketPrice", "currentPrice", "lastPrice", "close")) {
            Object val = source.get(field);
            if (val != null) {
                if (val instanceof Map<?, ?> valueMap) {
                    Object raw = valueMap.containsKey("raw") ? valueMap.get("raw") : valueMap.get("value");
                    if (raw != null) {
                        try {
                            return new BigDecimal(raw.toString());
                        } catch (NumberFormatException ignore) {
                            // try next field
                        }
                    }
                }
                try {
                    return new BigDecimal(val.toString());
                } catch (NumberFormatException ignore) {
                    // try next field
                }
            }
        }

        return null;
    }

    private String extractCurrency(String symbol, Map<String, Object> response) {
        if (response != null) {
            Object topLevelCurrency = response.get("currency");
            if (topLevelCurrency != null && !topLevelCurrency.toString().isBlank()) {
                return topLevelCurrency.toString().trim().toUpperCase();
            }

            Object quoteResponse = response.get("quoteResponse");
            if (quoteResponse instanceof Map<?, ?> quoteMap) {
                Object resultObj = quoteMap.get("result");
                if (resultObj instanceof List<?> resultList && !resultList.isEmpty()) {
                    Object first = resultList.get(0);
                    if (first instanceof Map<?, ?> firstQuote) {
                        Object resultCurrency = firstQuote.get("currency");
                        if (resultCurrency != null && !resultCurrency.toString().isBlank()) {
                            return resultCurrency.toString().trim().toUpperCase();
                        }
                    }
                }
            }

            Object chartObj = response.get("chart");
            if (chartObj instanceof Map<?, ?> chartMap) {
                Object chartResultObj = chartMap.get("result");
                if (chartResultObj instanceof List<?> chartResultList && !chartResultList.isEmpty()) {
                    Object firstChartResult = chartResultList.get(0);
                    if (firstChartResult instanceof Map<?, ?> chartResultMap) {
                        Object metaObj = chartResultMap.get("meta");
                        if (metaObj instanceof Map<?, ?> metaMap) {
                            Object chartCurrency = metaMap.get("currency");
                            if (chartCurrency != null && !chartCurrency.toString().isBlank()) {
                                return chartCurrency.toString().trim().toUpperCase();
                            }
                        }
                    }
                }
            }
        }

        // Suffix-based fallback for common Indian symbols.
        if (symbol.endsWith(".NS") || symbol.endsWith(".BO")) {
            return "INR";
        }
        return "USD";
    }

    private String defaultCurrencyForSymbol(String symbol) {
        return (symbol.endsWith(".NS") || symbol.endsWith(".BO")) ? "INR" : "USD";
    }

    private Map<String, String> buildCompanyNamesBySymbol() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("RELIANCE.NS", "Reliance Industries Ltd");
        names.put("TCS.NS", "Tata Consultancy Services Ltd");
        names.put("INFY.NS", "Infosys Ltd");
        names.put("HDFCBANK.NS", "HDFC Bank Ltd");
        names.put("ICICIBANK.NS", "ICICI Bank Ltd");
        names.put("SBIN.NS", "State Bank of India");
        names.put("LT.NS", "Larsen & Toubro Ltd");
        names.put("ITC.NS", "ITC Ltd");
        names.put("HINDUNILVR.NS", "Hindustan Unilever Ltd");
        names.put("AXISBANK.NS", "Axis Bank Ltd");
        names.put("BHARTIARTL.NS", "Bharti Airtel Ltd");
        names.put("KOTAKBANK.NS", "Kotak Mahindra Bank Ltd");
        names.put("ASIANPAINT.NS", "Asian Paints Ltd");
        names.put("BAJFINANCE.NS", "Bajaj Finance Ltd");
        names.put("BAJAJFINSV.NS", "Bajaj Finserv Ltd");
        names.put("MARUTI.NS", "Maruti Suzuki India Ltd");
        names.put("SUNPHARMA.NS", "Sun Pharmaceutical Industries Ltd");
        names.put("TITAN.NS", "Titan Company Ltd");
        names.put("ULTRACEMCO.NS", "UltraTech Cement Ltd");
        names.put("NESTLEIND.NS", "Nestle India Ltd");
        names.put("POWERGRID.NS", "Power Grid Corporation of India Ltd");
        names.put("NTPC.NS", "NTPC Ltd");
        names.put("TATAMOTORS.NS", "Tata Motors Ltd");
        names.put("M&M.NS", "Mahindra and Mahindra Ltd");
        names.put("WIPRO.NS", "Wipro Ltd");
        names.put("HCLTECH.NS", "HCL Technologies Ltd");
        names.put("TECHM.NS", "Tech Mahindra Ltd");
        names.put("ADANIENT.NS", "Adani Enterprises Ltd");
        names.put("ADANIPORTS.NS", "Adani Ports and Special Economic Zone Ltd");
        names.put("JSWSTEEL.NS", "JSW Steel Ltd");
        names.put("TATASTEEL.NS", "Tata Steel Ltd");
        names.put("HINDALCO.NS", "Hindalco Industries Ltd");
        names.put("ONGC.NS", "Oil and Natural Gas Corporation Ltd");
        names.put("COALINDIA.NS", "Coal India Ltd");
        names.put("INDUSINDBK.NS", "IndusInd Bank Ltd");
        names.put("DRREDDY.NS", "Dr. Reddy's Laboratories Ltd");
        names.put("CIPLA.NS", "Cipla Ltd");
        names.put("EICHERMOT.NS", "Eicher Motors Ltd");
        names.put("GRASIM.NS", "Grasim Industries Ltd");
        names.put("HEROMOTOCO.NS", "Hero MotoCorp Ltd");
        names.put("APOLLOHOSP.NS", "Apollo Hospitals Enterprise Ltd");
        names.put("BRITANNIA.NS", "Britannia Industries Ltd");
        names.put("DIVISLAB.NS", "Divi's Laboratories Ltd");
        names.put("BPCL.NS", "Bharat Petroleum Corporation Ltd");
        names.put("SHRIRAMFIN.NS", "Shriram Finance Ltd");
        names.put("SBILIFE.NS", "SBI Life Insurance Company Ltd");
        names.put("HDFCLIFE.NS", "HDFC Life Insurance Company Ltd");
        names.put("BAJAJ-AUTO.NS", "Bajaj Auto Ltd");
        names.put("TRENT.NS", "Trent Ltd");
        names.put("BEL.NS", "Bharat Electronics Ltd");
        names.put("HAL.NS", "Hindustan Aeronautics Ltd");
        names.put("IRCTC.NS", "Indian Railway Catering and Tourism Corporation Ltd");
        names.put("DMART.NS", "Avenue Supermarts Ltd");
        names.put("PIDILITIND.NS", "Pidilite Industries Ltd");
        names.put("DLF.NS", "DLF Ltd");
        names.put("SIEMENS.NS", "Siemens Ltd");
        names.put("GODREJCP.NS", "Godrej Consumer Products Ltd");
        names.put("ZOMATO.NS", "Eternal Ltd (formerly Zomato Ltd)");
        names.put("PAYTM.NS", "One 97 Communications Ltd");
        names.put("NYKAA.NS", "FSN E-Commerce Ventures Ltd");
        names.put("INDIGO.NS", "InterGlobe Aviation Ltd");
        return names;
    }

    private Optional<StockQuoteResponse> cacheAndBuildQuote(String symbol, BigDecimal price, String currency) {
        LocalDateTime now = LocalDateTime.now();
        cache.put(symbol, new CachedQuote(price, currency, now));
        log.info("Fetched live price for {}: {} {}", symbol, currency, price);
        return Optional.of(new StockQuoteResponse(symbol, price, currency, now));
    }
}
