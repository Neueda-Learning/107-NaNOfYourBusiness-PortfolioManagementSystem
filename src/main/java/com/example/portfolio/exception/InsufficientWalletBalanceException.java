package com.example.portfolio.exception;

public class InsufficientWalletBalanceException extends RuntimeException {

    public InsufficientWalletBalanceException(String message) {
        super(message);
    }
}

