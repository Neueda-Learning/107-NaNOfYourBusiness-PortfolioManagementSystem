package com.example.portfolio.controller;

import com.example.portfolio.dto.HoldingTradeRequest;
import com.example.portfolio.dto.PortfolioItemRequest;
import com.example.portfolio.dto.PortfolioItemResponse;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.service.PortfolioItemService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio-items")
public class PortfolioItemController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioItemController.class);

    private final PortfolioItemService service;

    public PortfolioItemController(PortfolioItemService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PortfolioItemResponse>> getAll(
            @RequestParam(required = false) AssetType type) {
        return ResponseEntity.ok(service.findAll(type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioItemResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<PortfolioItemResponse> create(
            @Valid @RequestBody PortfolioItemRequest request,
            UriComponentsBuilder ucb) {
        log.debug("Received create portfolio item request: type={}, symbolOrName={}", request.getType(), request.getSymbolOrName());
        PortfolioItemResponse created = service.create(request);
        URI location = ucb.path("/api/v1/portfolio-items/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioItemResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PortfolioItemRequest request) {
        log.debug("Received update portfolio item request: id={}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("Received delete portfolio item request: id={}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/refresh-price")
    public ResponseEntity<PortfolioItemResponse> refreshPrice(@PathVariable Long id) {
        return ResponseEntity.ok(service.refreshPrice(id));
    }

    @PostMapping("/{id}/buy")
    public ResponseEntity<PortfolioItemResponse> buy(@PathVariable Long id,
                                                     @Valid @RequestBody HoldingTradeRequest request) {
        return ResponseEntity.ok(service.buy(id, request.getQuantity()));
    }

    @PostMapping("/{id}/sell")
    public ResponseEntity<PortfolioItemResponse> sell(@PathVariable Long id,
                                                      @Valid @RequestBody HoldingTradeRequest request) {
        return ResponseEntity.ok(service.sell(id, request.getQuantity()));
    }
}
