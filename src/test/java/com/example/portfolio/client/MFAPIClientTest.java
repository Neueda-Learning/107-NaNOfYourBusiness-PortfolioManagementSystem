package com.example.portfolio.client;

import com.example.portfolio.exception.ExternalApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MFAPIClientTest {

    private StubHttpServer stub;

    @AfterEach
    void tearDown() {
        if (stub != null) {
            stub.close();
        }
    }

    @Test
    void getMutualFundDetails_withSuccessfulResponse_returnsParsedBody() {
        stub = StubHttpServer.startReturning(200, "application/json", """
                {
                  "meta": { "scheme_name": "HDFC Flexi Cap Fund" },
                  "data": [ { "date": "05-08-2026", "nav": "650.25" } ]
                }
                """);
        MFAPIClient client = new MFAPIClient(stub.baseUrl());

        Map<String, Object> response = client.getMutualFundDetails(119551);

        assertThat(response).containsKey("meta");
        assertThat(response).containsKey("data");
    }

    @Test
    void getMutualFundDetails_whenServerReturnsError_throwsExternalApiException() {
        stub = StubHttpServer.startReturning(500, "application/json", "{\"error\":\"boom\"}");
        MFAPIClient client = new MFAPIClient(stub.baseUrl());

        assertThatThrownBy(() -> client.getMutualFundDetails(119551))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("119551");
    }


    @Test
    void extractLatestNav_withValidResponse_returnsFirstNavEntry() {
        MFAPIClient client = new MFAPIClient("http://localhost:9999");
        Map<String, Object> response = Map.of(
                "data", java.util.List.of(Map.of("date", "05-08-2026", "nav", "650.25"))
        );

        BigDecimal nav = client.extractLatestNav(response);

        assertThat(nav).isEqualByComparingTo("650.25");
    }

    @Test
    void extractLatestNav_withEmptyDataList_throwsExternalApiException() {
        MFAPIClient client = new MFAPIClient("http://localhost:9999");
        Map<String, Object> response = Map.of("data", java.util.List.of());

        assertThatThrownBy(() -> client.extractLatestNav(response))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Could not extract NAV");
    }

    @Test
    void extractLatestNav_withMissingDataKey_throwsExternalApiException() {
        MFAPIClient client = new MFAPIClient("http://localhost:9999");

        assertThatThrownBy(() -> client.extractLatestNav(Map.of()))
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    void extractLatestNav_withMalformedNav_throwsExternalApiException() {
        MFAPIClient client = new MFAPIClient("http://localhost:9999");
        Map<String, Object> response = Map.of(
                "data", java.util.List.of(Map.of("date", "05-08-2026", "nav", "not-a-number"))
        );

        assertThatThrownBy(() -> client.extractLatestNav(response))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Invalid NAV format");
    }

    @Test
    void extractSchemeName_withPresentMeta_returnsName() {
        MFAPIClient client = new MFAPIClient("http://localhost:9999");
        Map<String, Object> response = Map.of("meta", Map.of("scheme_name", "HDFC Flexi Cap Fund"));

        assertThat(client.extractSchemeName(response)).isEqualTo("HDFC Flexi Cap Fund");
    }

    @Test
    void extractSchemeName_withMissingMeta_returnsUnknown() {
        MFAPIClient client = new MFAPIClient("http://localhost:9999");

        assertThat(client.extractSchemeName(Map.of())).isEqualTo("Unknown");
    }
}

