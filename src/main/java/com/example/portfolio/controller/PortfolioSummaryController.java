package com.example.portfolio.controller;

import com.example.portfolio.dto.PortfolioPerformanceResponse;
import com.example.portfolio.dto.PortfolioSummaryResponse;
import com.example.portfolio.service.PortfolioPerformanceService;
import com.example.portfolio.service.PortfolioSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioSummaryController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioSummaryController.class);

    private final PortfolioSummaryService service;
    private final PortfolioPerformanceService performanceService;

    public PortfolioSummaryController(PortfolioSummaryService service,
                                      PortfolioPerformanceService performanceService) {
        this.service = service;
        this.performanceService = performanceService;
    }

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryResponse> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }

    @GetMapping("/performance")
    public ResponseEntity<PortfolioPerformanceResponse> getPerformance(
            @RequestParam(required = false, defaultValue = "ALL") String range) {
        log.debug("Received portfolio performance request: range={}", range);
        return ResponseEntity.ok(performanceService.getPerformance(range));
    }
}

