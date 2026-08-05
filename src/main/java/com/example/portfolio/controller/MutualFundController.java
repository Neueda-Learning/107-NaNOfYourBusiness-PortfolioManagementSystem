package com.example.portfolio.controller;

import com.example.portfolio.dto.BuyMutualFundRequest;
import com.example.portfolio.dto.MutualFundHistoryResponse;
import com.example.portfolio.dto.MutualFundSummaryResponse;
import com.example.portfolio.dto.SellMutualFundRequest;
import com.example.portfolio.service.MutualFundService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mutual-funds")
public class MutualFundController {

    private final MutualFundService mutualFundService;

    public MutualFundController(MutualFundService mutualFundService) {
        this.mutualFundService = mutualFundService;
    }

    /**
     * GET /api/mutual-funds
     * Returns all 30 supported mutual funds with latest NAV
     */
    @GetMapping
    public ResponseEntity<List<MutualFundSummaryResponse>> getSupportedFunds() {
        return ResponseEntity.ok(mutualFundService.getSupportedFunds());
    }

    /**
     * GET /api/mutual-funds/{schemeCode}
     * Returns raw MFAPI response for a specific fund
     */
    @GetMapping("/{schemeCode}")
    public ResponseEntity<Map<String, Object>> getMutualFundDetails(@PathVariable Integer schemeCode) {
        return ResponseEntity.ok(mutualFundService.getMutualFundDetails(schemeCode));
    }

    /**
     * GET /api/mutual-funds/{schemeCode}/history?range=1M|3M|6M|1Y|ALL
     * Returns NAV history for charting
     */
    @GetMapping("/{schemeCode}/history")
    public ResponseEntity<MutualFundHistoryResponse> getMutualFundHistory(
            @PathVariable Integer schemeCode,
            @RequestParam(required = false, defaultValue = "ALL") String range) {
        return ResponseEntity.ok(mutualFundService.getMutualFundHistory(schemeCode, range));
    }

    /**
     * POST /api/mutual-funds/buy
     * Buy mutual fund using amount (units calculated internally)
     */
    @PostMapping("/buy")
    public ResponseEntity<Map<String, Object>> buyMutualFund(
            @Valid @RequestBody BuyMutualFundRequest request) {
        return ResponseEntity.ok(mutualFundService.buyMutualFund(request));
    }

    /**
     * POST /api/mutual-funds/sell
     * Sell mutual fund using amount (units calculated from current NAV)
     */
    @PostMapping("/sell")
    public ResponseEntity<Map<String, Object>> sellMutualFund(
            @Valid @RequestBody SellMutualFundRequest request) {
        return ResponseEntity.ok(mutualFundService.sellMutualFund(request));
    }
}

