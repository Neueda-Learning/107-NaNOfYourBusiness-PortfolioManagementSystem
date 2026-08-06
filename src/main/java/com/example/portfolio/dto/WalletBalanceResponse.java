package com.example.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletBalanceResponse {

    private BigDecimal balance;
    private LocalDateTime updatedAt;

    public WalletBalanceResponse(BigDecimal balance, LocalDateTime updatedAt) {
        this.balance = balance;
        this.updatedAt = updatedAt;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

