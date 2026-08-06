package com.example.portfolio.exception;

public class BondRedemptionException extends RuntimeException {

    private final String errorCode;

    public BondRedemptionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
