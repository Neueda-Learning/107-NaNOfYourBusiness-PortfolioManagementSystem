package com.example.portfolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", e.getDefaultMessage()))
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("error", "VALIDATION_ERROR");
        body.put("message", "Validation failed");
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return badRequest("VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return badRequest("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(InsufficientWalletBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientWalletBalance(InsufficientWalletBalanceException ex) {
        return badRequest("INSUFFICIENT_WALLET_BALANCE", ex.getMessage());
    }

    @ExceptionHandler(BondRedemptionException.class)
    public ResponseEntity<Map<String, Object>> handleBondRedemption(BondRedemptionException ex) {
        return error(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<Map<String, Object>> handleExternalApi(ExternalApiException ex) {
        return error(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", ex.getMessage());
    }

    /**
     * Missing static resources (e.g. browsers auto-requesting /favicon.ico) are normal,
     * harmless 404s — not application errors. Handle separately so they don't get logged
     * as ERROR-level stack traces by the catch-all handler below.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("Static resource not found: {}", ex.getResourcePath());
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found: " + ex.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> badRequest(String errorCode, String message) {
        return error(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String errorCode, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", errorCode);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
