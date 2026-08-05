package com.example.portfolio.config;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class MutualFundCatalogue {

    private final Map<Integer, String> supportedFunds;

    public MutualFundCatalogue() {
        // Initialize with 30 popular mutual funds
        Map<Integer, String> funds = new HashMap<>();
        funds.put(119551, "HDFC Flexi Cap Fund");
        funds.put(119552, "HDFC Top 100 Fund");
        funds.put(119553, "SBI Bluechip Fund");
        funds.put(119554, "SBI Equity Hybrid Fund");
        funds.put(119555, "ICICI Prudential Bluechip Fund");
        funds.put(119556, "Axis Bluechip Fund");
        funds.put(119557, "Nippon India Growth Fund");
        funds.put(119558, "Parag Parikh Flexi Cap Fund");
        funds.put(119559, "Mirae Asset Large Cap Fund");
        funds.put(119560, "Kotak Equity Opportunities Fund");
        funds.put(119561, "Aditya Birla Sun Life Focused Equity Fund");
        funds.put(119562, "Franklin India Focused Equity Fund");
        funds.put(119563, "ICICI Prudential Value Discovery Fund");
        funds.put(119564, "Motilal Oswal Large Cap Fund");
        funds.put(119565, "Canara Robeco Equity Diversified Fund");
        funds.put(119566, "HDFC Core Equity Fund");
        funds.put(119567, "IDFC Core Equity Fund");
        funds.put(119568, "DSP Equal Weight NSE 50 Fund");
        funds.put(119569, "L&T Focused Equity Fund");
        funds.put(119570, "SBI Focused Equity Fund");
        funds.put(119571, "HDFC Mid-Cap Opportunities Fund");
        funds.put(119572, "ICICI Prudential Mid Cap Fund");
        funds.put(119573, "Axis Midcap Fund");
        funds.put(119574, "Nippon India Mid Cap Fund");
        funds.put(119575, "Motilal Oswal Mid Cap 30 Fund");
        funds.put(119576, "JM Core 11 Fund");
        funds.put(119577, "Edelweiss Large Cap Fund");
        funds.put(119578, "HSBC Large Cap Equity Fund");
        funds.put(119579, "PGIM India Large Cap Fund");
        funds.put(119580, "UTI Equity Fund");

        this.supportedFunds = Collections.unmodifiableMap(funds);
    }

    /**
     * Check if a scheme code is supported
     */
    public boolean isSupported(Integer schemeCode) {
        return supportedFunds.containsKey(schemeCode);
    }

    /**
     * Get scheme name for a code
     */
    public String getSchemeName(Integer schemeCode) {
        return supportedFunds.get(schemeCode);
    }

    /**
     * Get all supported scheme codes
     */
    public Set<Integer> getAllSchemeCodes() {
        return supportedFunds.keySet();
    }

    /**
     * Get all supported funds as a map
     */
    public Map<Integer, String> getAllFunds() {
        return supportedFunds;
    }
}

