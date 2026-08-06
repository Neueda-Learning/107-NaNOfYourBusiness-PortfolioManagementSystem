package com.example.portfolio.client;

import com.example.portfolio.dto.StockHistoryPoint;
import com.example.portfolio.exception.ExternalApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TwelveDataClientTest {

    private StubHttpServer stub;

    @AfterEach
    void tearDown() {
        if (stub != null) {
            stub.close();
        }
    }

    private TwelveDataClient clientFor(StubHttpServer server) {
        RestClient restClient = RestClient.builder().baseUrl(server.baseUrl()).build();
        return new TwelveDataClient(restClient);
    }

    @Test
    void getDailyHistory_withValidResponse_returnsChronologicallyOrderedPoints() {
        // Twelve Data returns most-recent-first; client must reverse to ascending order.
        stub = StubHttpServer.startReturning(200, "application/json", """
                {
                  "status": "ok",
                  "values": [
                    { "datetime": "2026-08-05", "open": "10", "high": "12", "low": "9", "close": "11", "volume": "1000" },
                    { "datetime": "2026-08-04", "open": "9",  "high": "10", "low": "8", "close": "9.5", "volume": "900" }
                  ]
                }
                """);

        List<StockHistoryPoint> points = clientFor(stub).getDailyHistory("AAPL", "1day", 30);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).getDate()).isEqualTo("2026-08-04");
        assertThat(points.get(0).getClose()).isEqualByComparingTo("9.5");
        assertThat(points.get(1).getDate()).isEqualTo("2026-08-05");
        assertThat(points.get(1).getClose()).isEqualByComparingTo("11");
    }

    @Test
    void getDailyHistory_skipsMalformedRowsButKeepsValidOnes() {
        stub = StubHttpServer.startReturning(200, "application/json", """
                {
                  "status": "ok",
                  "values": [
                    { "datetime": "2026-08-05", "close": "11" },
                    { "datetime": "not-a-date", "close": "9.5" }
                  ]
                }
                """);

        List<StockHistoryPoint> points = clientFor(stub).getDailyHistory("AAPL", "1day", 30);

        assertThat(points).hasSize(1);
        assertThat(points.get(0).getDate()).isEqualTo("2026-08-05");
    }

    @Test
    void getDailyHistory_withRowMissingClose_skipsRow() {
        stub = StubHttpServer.startReturning(200, "application/json", """
                {
                  "status": "ok",
                  "values": [
                    { "datetime": "2026-08-05", "open": "10" }
                  ]
                }
                """);

        assertThatThrownBy(() -> clientFor(stub).getDailyHistory("AAPL", "1day", 30))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Could not parse any history rows");
    }

    @Test
    void getDailyHistory_withErrorStatus_throwsExternalApiException() {
        stub = StubHttpServer.startReturning(200, "application/json", """
                { "status": "error", "message": "Invalid symbol" }
                """);

        assertThatThrownBy(() -> clientFor(stub).getDailyHistory("BOGUS", "1day", 30))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Invalid symbol");
    }

    @Test
    void getDailyHistory_withEmptyValues_throwsExternalApiException() {
        stub = StubHttpServer.startReturning(200, "application/json", """
                { "status": "ok", "values": [] }
                """);

        assertThatThrownBy(() -> clientFor(stub).getDailyHistory("AAPL", "1day", 30))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("No history values");
    }

    @Test
    void getDailyHistory_withMissingValuesKey_throwsExternalApiException() {
        stub = StubHttpServer.startReturning(200, "application/json", """
                { "status": "ok" }
                """);

        assertThatThrownBy(() -> clientFor(stub).getDailyHistory("AAPL", "1day", 30))
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    void getDailyHistory_whenServerUnreachable_throwsExternalApiException() {
        // TEST-NET-1 (RFC 5737) is reserved/non-routable, so the connection attempt
        // will time out rather than being refused instantly — use a short explicit
        // connect/read timeout so this test fails fast instead of hanging.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1000);
        RestClient restClient = RestClient.builder()
                .baseUrl("http://192.0.2.1:9")
                .requestFactory(factory)
                .build();
        TwelveDataClient client = new TwelveDataClient(restClient);

        assertThatThrownBy(() -> client.getDailyHistory("AAPL", "1day", 30))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void getDailyHistory_whenServerReturns500_throwsExternalApiException() {
        stub = StubHttpServer.startReturning(500, "application/json", "{}");

        assertThatThrownBy(() -> clientFor(stub).getDailyHistory("AAPL", "1day", 30))
                .isInstanceOf(ExternalApiException.class);
    }
}

