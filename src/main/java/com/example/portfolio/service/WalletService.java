package com.example.portfolio.service;

import com.example.portfolio.dto.WalletBalanceResponse;
import com.example.portfolio.dto.WalletTransactionResponse;
import com.example.portfolio.exception.InsufficientWalletBalanceException;
import com.example.portfolio.model.AssetType;
import com.example.portfolio.model.WalletTransactionType;
import com.example.portfolio.repository.UserDataRepository;
import com.example.portfolio.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletService {

    private final UserDataRepository userDataRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(UserDataRepository userDataRepository,
                         WalletTransactionRepository walletTransactionRepository) {
        this.userDataRepository = userDataRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    public WalletBalanceResponse getBalance() {
        Long userId = userDataRepository.getSingleUserId();
        return new WalletBalanceResponse(userDataRepository.getWalletBalance(userId), LocalDateTime.now());
    }

    @Transactional
    public WalletBalanceResponse deposit(BigDecimal amount) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        Long userId = userDataRepository.getSingleUserId();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal updatedBalance = userDataRepository.increaseWalletBalance(userId, normalizedAmount, now);

        walletTransactionRepository.saveTransaction(
                userId,
                WalletTransactionType.DEPOSIT,
                normalizedAmount,
                updatedBalance,
                null,
                null,
                null,
                now);

        return new WalletBalanceResponse(updatedBalance, now);
    }

    @Transactional
    public void debitForBuy(BigDecimal amount, AssetType assetType, Long portfolioItemId, String symbolOrName) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        Long userId = userDataRepository.getSingleUserId();
        LocalDateTime now = LocalDateTime.now();

        boolean debited = userDataRepository.decreaseWalletBalanceIfSufficient(userId, normalizedAmount, now);
        if (!debited) {
            BigDecimal currentBalance = userDataRepository.getWalletBalance(userId);
            throw new InsufficientWalletBalanceException(
                    "Insufficient wallet balance. Available: " + currentBalance + ", required: " + normalizedAmount);
        }

        BigDecimal updatedBalance = userDataRepository.getWalletBalance(userId);
        walletTransactionRepository.saveTransaction(
                userId,
                WalletTransactionType.BUY_DEBIT,
                normalizedAmount,
                updatedBalance,
                assetType,
                portfolioItemId,
                symbolOrName,
                now);
    }

    @Transactional
    public void creditForSell(BigDecimal amount, AssetType assetType, Long portfolioItemId, String symbolOrName) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        Long userId = userDataRepository.getSingleUserId();
        LocalDateTime now = LocalDateTime.now();

        BigDecimal updatedBalance = userDataRepository.increaseWalletBalance(userId, normalizedAmount, now);
        walletTransactionRepository.saveTransaction(
                userId,
                WalletTransactionType.SELL_CREDIT,
                normalizedAmount,
                updatedBalance,
                assetType,
                portfolioItemId,
                symbolOrName,
                now);
    }

    public List<WalletTransactionResponse> getTransactionHistory() {
        Long userId = userDataRepository.getSingleUserId();
        return walletTransactionRepository.findByUserId(userId).stream()
                .map(record -> new WalletTransactionResponse(
                        record.id(),
                        record.type().name(),
                        record.amount(),
                        record.balanceAfter(),
                        record.assetType() != null ? record.assetType().name() : null,
                        record.portfolioItemId(),
                        record.symbolOrName(),
                        record.createdAt()
                ))
                .toList();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        return amount.setScale(4, RoundingMode.HALF_UP);
    }
}

