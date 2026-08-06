package com.example.portfolio.controller;

import com.example.portfolio.dto.WalletBalanceResponse;
import com.example.portfolio.dto.WalletDepositRequest;
import com.example.portfolio.dto.WalletTransactionResponse;
import com.example.portfolio.service.WalletService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletBalanceResponse> getBalance() {
        return ResponseEntity.ok(walletService.getBalance());
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletBalanceResponse> deposit(@Valid @RequestBody WalletDepositRequest request) {
        log.debug("Received wallet deposit request: amount={}", request.getAmount());
        return ResponseEntity.ok(walletService.deposit(request.getAmount()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransactionResponse>> getTransactionHistory() {
        return ResponseEntity.ok(walletService.getTransactionHistory());
    }
}

