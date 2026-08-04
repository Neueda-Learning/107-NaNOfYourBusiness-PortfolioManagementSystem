package com.example.portfolio.service;

import com.example.portfolio.exception.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MarketDataServiceTest {

    private MockRestServiceServer server;
    private MarketDataService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.test");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new MarketDataService(builder.build(), "AAPL,AMZN,TSLA");
    }

    @Test
    void getSupportedTickers_returnsConfiguredList() {
        assertThat(service.getSupportedTickers()).containsExactly("AAPL", "AMZN", "TSLA");
    }

    @Test
    void getQuote_blankTicker_returnsEmpty() {
        assertThat(service.getQuote("   ")).isEmpty();
    }

    @Test
    void getQuote_readsFlatPriceAndCachesResult() {
        server.expect(requestTo("https://example.test/cachedPriceData?ticker=TSLA"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"price\":248.13}", MediaType.APPLICATION_JSON));

        var first = service.getQuote("TSLA");
        var second = service.getQuote("TSLA");

        assertThat(first).isPresent();
        assertThat(first.get().getPrice()).isEqualByComparingTo("248.13");
        assertThat(second).isPresent();
        assertThat(second.get().getPrice()).isEqualByComparingTo("248.13");
        server.verify();
    }

    @Test
    void fetchPriceOrThrow_throwsWhenPriceCannotBeExtracted() {
        server.expect(requestTo("https://example.test/cachedPriceData?ticker=TSLA"))
                .andRespond(withSuccess("{\"ticker\":\"TSLA\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.fetchPriceOrThrow("TSLA"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("TSLA");
    }

    @Test
    void fetchPrice_returnsEmptyWhenUpstreamPayloadIsInvalid() {
        server.expect(requestTo("https://example.test/cachedPriceData?ticker=AAPL"))
                .andRespond(withSuccess("{\"unexpected\":true}", MediaType.APPLICATION_JSON));

        Optional<?> result = service.fetchPrice("AAPL");

        assertThat(result).isEmpty();
    }
}

