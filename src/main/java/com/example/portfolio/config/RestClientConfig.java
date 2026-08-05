package com.example.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${market.api.base-url}")
    private String marketApiBaseUrl;

    @Value("${finnhub.base-url:https://finnhub.io/api/v1}")
    private String finnhubBaseUrl;

    @Value("${finnhub.api-key:}")
    private String finnhubApiKey;

    @Bean
    public RestClient marketRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

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
        }

        return builder.build();
    }
}
