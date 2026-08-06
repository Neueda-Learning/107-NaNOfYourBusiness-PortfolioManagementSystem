package com.example.portfolio.dto;

import java.util.List;

/** Response body for GET /api/v1/portfolio/performance */
public class PortfolioPerformanceResponse {
    private String range;
    private List<PortfolioPerformancePoint> points;

    public PortfolioPerformanceResponse() {
    }

    public PortfolioPerformanceResponse(String range, List<PortfolioPerformancePoint> points) {
        this.range = range;
        this.points = points;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public List<PortfolioPerformancePoint> getPoints() {
        return points;
    }

    public void setPoints(List<PortfolioPerformancePoint> points) {
        this.points = points;
    }
}

