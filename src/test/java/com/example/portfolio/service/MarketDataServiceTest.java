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
        service = new MarketDataService(builder.build(), "TCS.NS,INFY.NS,RELIANCE.NS", "", false, false);
    }

    @Test
    void getSupportedTickers_returnsConfiguredList() {
        assertThat(service.getSupportedTickers()).containsExactly("TCS.NS", "INFY.NS", "RELIANCE.NS");
    }

    @Test
    void getStockCatalog_returnsSymbolCompanyNameAndCurrency() {
        var catalog = service.getStockCatalog();

        assertThat(catalog).hasSize(3);
        assertThat(catalog.get(0).getSymbol()).isEqualTo("TCS.NS");
        assertThat(catalog.get(0).getCompanyName()).isEqualTo("Tata Consultancy Services Ltd");
        assertThat(catalog.get(0).getCurrency()).isEqualTo("INR");
    }

    @Test
    void getQuote_blankTicker_returnsEmpty() {
        assertThat(service.getQuote("   ")).isEmpty();
    }

    @Test
    void getQuote_readsFlatPriceAndCachesResult() {
        server.expect(requestTo("https://example.test/cachedPriceData?ticker=TCS.NS"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"price\":248.13}", MediaType.APPLICATION_JSON));

        var first = service.getQuote("TCS");
        var second = service.getQuote("TCS");

        assertThat(first).isPresent();
        assertThat(first.get().getPrice()).isEqualByComparingTo("248.13");
        assertThat(second).isPresent();
        assertThat(second.get().getPrice()).isEqualByComparingTo("248.13");
        server.verify();
    }

    @Test
    void fetchPriceOrThrow_throwsWhenPriceCannotBeExtracted() {
        server.expect(requestTo("https://example.test/cachedPriceData?ticker=TCS.NS"))
                .andRespond(withSuccess("{\"ticker\":\"TCS.NS\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.fetchPriceOrThrow("TCS"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("TCS.NS");
    }

    @Test
    void fetchPrice_returnsEmptyWhenUpstreamPayloadIsInvalid() {
        server.expect(requestTo("https://example.test/cachedPriceData?ticker=INFY.NS"))
                .andRespond(withSuccess("{\"unexpected\":true}", MediaType.APPLICATION_JSON));

        Optional<?> result = service.fetchPrice("INFY");

        assertThat(result).isEmpty();
    }

    @Test
    void getQuote_unsupportedTicker_returnsEmpty() {
        assertThat(service.getQuote("TSLA")).isEmpty();
    }
}

