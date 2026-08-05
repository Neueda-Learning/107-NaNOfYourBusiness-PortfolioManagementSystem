package com.example.portfolio.client;

import com.example.portfolio.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class MFAPIClient {

    private static final Logger log = LoggerFactory.getLogger(MFAPIClient.class);

    private final RestClient restClient;

    public MFAPIClient(@Value("${mfapi.base-url:https://api.mfapi.in}") String mfapiBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(mfapiBaseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Fetch mutual fund details from MFAPI
     * Returns raw response as Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMutualFundDetails(Integer schemeCode) {
        try {
            log.debug("Fetching MFAPI details for scheme code: {}", schemeCode);
            Map<String, Object> response = restClient.get()
                    .uri("/mf/{schemeCode}", schemeCode)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new ExternalApiException("Empty response from MFAPI for scheme code: " + schemeCode);
            }

            log.info("Successfully fetched MFAPI data for scheme code: {}", schemeCode);
            return response;
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch MFAPI data for scheme code: {}", schemeCode, e);
            throw new ExternalApiException(
                    "Mutual fund provider unavailable for scheme code: " + schemeCode, e);
        }
    }

    /**
     * Extract latest NAV from MFAPI response
     * MFAPI format: { "meta": {...}, "data": [{"date": "...", "nav": "650.25"}, ...] }
     */
    public BigDecimal extractLatestNav(Map<String, Object> mfapiResponse) {
        try {
            Object dataObj = mfapiResponse.get("data");
            if (dataObj instanceof java.util.List<?> dataList && !dataList.isEmpty()) {
                Object firstEntry = dataList.get(0);
                if (firstEntry instanceof Map<?, ?> entryMap) {
                    Object navObj = entryMap.get("nav");
                    if (navObj != null) {
                        return new BigDecimal(navObj.toString());
                    }
                }
            }
            throw new ExternalApiException("Could not extract NAV from MFAPI response");
        } catch (NumberFormatException e) {
            throw new ExternalApiException("Invalid NAV format in MFAPI response", e);
        }
    }

    /**
     * Extract scheme name from MFAPI response
     */
    public String extractSchemeName(Map<String, Object> mfapiResponse) {
        Object metaObj = mfapiResponse.get("meta");
        if (metaObj instanceof Map<?, ?> metaMap) {
            Object schemeNameObj = metaMap.get("scheme_name");
            if (schemeNameObj != null) {
                return schemeNameObj.toString();
            }
        }
        return "Unknown";
    }
}

