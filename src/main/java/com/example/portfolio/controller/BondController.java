package com.example.portfolio.controller;

import com.example.portfolio.service.BondService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bonds")
public class BondController {

    private final BondService bondService;

    public BondController(BondService bondService) {
        this.bondService = bondService;
    }

    @GetMapping
    public ResponseEntity<List<BondService.BondResponse>> getAllBonds() {
        return ResponseEntity.ok(bondService.getAllBonds());
    }

    @GetMapping("/all")
    public ResponseEntity<List<BondService.BondResponse>> getBondCatalog() {
        return ResponseEntity.ok(bondService.getBondCatalog());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<BondService.BondResponse> getBondDetails(@PathVariable Long id) {
        return ResponseEntity.ok(bondService.getBondDetails(id));
    }

    @GetMapping("/redeemed")
    public ResponseEntity<List<BondService.BondResponse>> getRedeemedBonds() {
        return ResponseEntity.ok(bondService.getRedeemedBonds());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BondService.BondResponse>> searchBonds(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String issuer,
            @RequestParam(required = false) String creditRating,
            @RequestParam(required = false) java.time.LocalDate maturityFrom,
            @RequestParam(required = false) java.time.LocalDate maturityTo) {
        return ResponseEntity.ok(bondService.searchBonds(query, issuer, creditRating, maturityFrom, maturityTo));
    }

    @PostMapping("/buy")
    public ResponseEntity<BondService.BondResponse> buyBond(
            @Valid @RequestBody BondService.BondTradeRequest request) {
        return ResponseEntity.ok(bondService.buyBond(request));
    }

    @PostMapping("/redeem")
    public ResponseEntity<BondService.BondResponse> redeemBond(
            @Valid @RequestBody BondService.BondRedeemRequest request) {
        return ResponseEntity.ok(bondService.redeemBond(request));
    }
}
