package com.example.portfolio.controller;

import com.example.portfolio.dto.PortfolioSummaryResponse;
import com.example.portfolio.service.PortfolioSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioSummaryController {

    private final PortfolioSummaryService service;

    public PortfolioSummaryController(PortfolioSummaryService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryResponse> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }
}
