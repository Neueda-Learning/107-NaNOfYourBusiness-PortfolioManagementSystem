package com.example.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Configuration
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    @Value("${market.api.base-url}")
    private String marketApiBaseUrl;

    @Value("${finnhub.base-url:https://finnhub.io/api/v1}")
    private String finnhubBaseUrl;

    @Value("${finnhub.api-key:}")
    private String finnhubApiKey;

    @Value("${twelvedata.base-url:https://api.twelvedata.com}")
    private String twelveDataBaseUrl;

    @Value("${twelvedata.api-key:}")
    private String twelveDataApiKey;

    @Bean
    public RestClient marketRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        log.info("Configuring market RestClient with baseUrl={}", marketApiBaseUrl);
        return RestClient.builder()
                .baseUrl(marketApiBaseUrl)
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestClient finnhubRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(finnhubBaseUrl)
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json");

        if (finnhubApiKey != null && !finnhubApiKey.isBlank()) {
            builder.defaultHeader("X-Finnhub-Token", finnhubApiKey.trim());
        } else {
            log.warn("Finnhub API key is not configured; Finnhub requests may be rejected");
        }

        log.info("Configuring Finnhub RestClient with baseUrl={}", finnhubBaseUrl);
        return builder.build();
    }

    /**
     * RestClient for Twelve Data's historical chart endpoint (time_series).
     * The apikey is attached as a query param on every request via an interceptor,
     * so individual client calls never need to remember to add it themselves.
     */
    @Bean
    public RestClient twelveDataRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        if (twelveDataApiKey == null || twelveDataApiKey.isBlank()) {
            log.warn("Twelve Data API key is not configured; history requests may be rejected");
        }
        log.info("Configuring Twelve Data RestClient with baseUrl={}", twelveDataBaseUrl);
        return RestClient.builder()
                .baseUrl(twelveDataBaseUrl)
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .requestInterceptor((request, body, execution) -> {
                    URI original = request.getURI();
                    String existingQuery = original.getRawQuery();
                    String apiKeyParam = "apikey=" + (twelveDataApiKey == null ? "" : twelveDataApiKey.trim());
                    String newQuery = (existingQuery == null || existingQuery.isBlank())
                            ? apiKeyParam
                            : existingQuery + "&" + apiKeyParam;
                    URI newUri = UriComponentsBuilder.fromUri(original)
                            .replaceQuery(newQuery)
                            .build(true)
                            .toUri();
                    return execution.execute(new org.springframework.http.client.support.HttpRequestWrapper(request) {
                        @Override
                        public URI getURI() {
                            return newUri;
                        }
                    }, body);
                })
                .build();
    }
}
