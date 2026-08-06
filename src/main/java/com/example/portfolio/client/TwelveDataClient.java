package com.example.portfolio.client;

import com.example.portfolio.dto.StockHistoryPoint;
import com.example.portfolio.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fetches daily OHLCV history for US-listed tickers from Twelve Data's
 * {@code /time_series} endpoint. Used only for the "Price History" chart —
 * never for live quotes (those stay on the Finnhub cache in MarketDataService).
 *
 * Free tier: 800 credits/day, capped at 8 calls/minute. Called on-demand when a
 * user opens a chart, so no polling/caching pattern is needed here.
 */
@Component
public class TwelveDataClient {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataClient.class);

    private final RestClient restClient;

    public TwelveDataClient(@Qualifier("twelveDataRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Fetch daily history for a US ticker (e.g. "AAPL").
     * Returns points sorted ascending (chronological) by date — Twelve Data
     * returns most-recent-first, so the raw list is reversed before returning.
     * Throws ExternalApiException on any network failure, non-200, "status":"error"
     * body, or malformed/empty response.
     */
    public List<StockHistoryPoint> getDailyHistory(String symbol, String interval, int outputSize) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/time_series")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("outputsize", outputSize)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (body == null) {
                throw new ExternalApiException("Empty response from Twelve Data for symbol: " + symbol);
            }

            Object status = body.get("status");
            if ("error".equals(status)) {
                String message = String.valueOf(body.getOrDefault("message", "Unknown error"));
                throw new ExternalApiException("Twelve Data error for " + symbol + ": " + message);
            }

            Object valuesObj = body.get("values");
            if (!(valuesObj instanceof List<?> valuesList) || valuesList.isEmpty()) {
                throw new ExternalApiException("No history values returned by Twelve Data for symbol: " + symbol);
            }

            List<StockHistoryPoint> points = new ArrayList<>();
            for (Object entry : valuesList) {
                if (entry instanceof Map<?, ?> row) {
                    StockHistoryPoint point = parseRow(row);
                    if (point != null) {
                        points.add(point);
                    }
                }
            }

            if (points.isEmpty()) {
                throw new ExternalApiException("Could not parse any history rows for symbol: " + symbol);
            }

            // Twelve Data returns most-recent-first; reverse for chronological (ascending) order.
            Collections.reverse(points);

            log.info("Fetched {} daily history points for {} from Twelve Data", points.size(), symbol);
            return points;
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to fetch Twelve Data history for {}: {}", symbol, e.getMessage());
            throw new ExternalApiException("Stock history provider unavailable for symbol: " + symbol, e);
        }
    }

    private StockHistoryPoint parseRow(Map<?, ?> row) {
        try {
            String datetime = String.valueOf(row.get("datetime"));
            // Daily interval returns "YYYY-MM-DD"; intraday would include a time
            // component — take just the date portion since our model is daily-only.
            LocalDate date = LocalDate.parse(datetime.length() >= 10 ? datetime.substring(0, 10) : datetime);

            BigDecimal open = parseDecimal(row.get("open"));
            BigDecimal high = parseDecimal(row.get("high"));
            BigDecimal low = parseDecimal(row.get("low"));
            BigDecimal close = parseDecimal(row.get("close"));
            BigDecimal volume = parseDecimal(row.get("volume"));

            if (close == null) {
                return null;
            }
            return new StockHistoryPoint(date, open, high, low, close, volume);
        } catch (Exception ignore) {
            return null; // skip malformed row
        }
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

