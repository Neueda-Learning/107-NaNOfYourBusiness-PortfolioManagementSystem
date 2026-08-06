package com.example.portfolio.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public class UserDataRepository {

    private final JdbcTemplate jdbc;

    public UserDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long getSingleUserId() {
        Long id = jdbc.queryForObject(
                "SELECT id FROM user_data ORDER BY id LIMIT 1",
                Long.class);
        if (id == null) {
            throw new IllegalStateException("No user_data row found");
        }
        return id;
    }

    public BigDecimal getWalletBalance(Long userId) {
        BigDecimal balance = jdbc.queryForObject(
                "SELECT wallet_balance FROM user_data WHERE id = ?",
                BigDecimal.class,
                userId);
        if (balance == null) {
            throw new IllegalStateException("Wallet balance not found for user id: " + userId);
        }
        return balance;
    }

    public BigDecimal increaseWalletBalance(Long userId, BigDecimal amount, LocalDateTime updatedAt) {
        jdbc.update(
                "UPDATE user_data SET wallet_balance = wallet_balance + ?, updated_at = ? WHERE id = ?",
                amount,
                updatedAt,
                userId);
        return getWalletBalance(userId);
    }

    public boolean decreaseWalletBalanceIfSufficient(Long userId, BigDecimal amount, LocalDateTime updatedAt) {
        int rows = jdbc.update(
                "UPDATE user_data SET wallet_balance = wallet_balance - ?, updated_at = ? WHERE id = ? AND wallet_balance >= ?",
                amount,
                updatedAt,
                userId,
                amount);
        return rows == 1;
    }
}

