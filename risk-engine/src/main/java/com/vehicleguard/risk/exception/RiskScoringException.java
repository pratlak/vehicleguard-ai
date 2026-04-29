package com.vehicleguard.risk.exception;

public class RiskScoringException extends RuntimeException {
    public RiskScoringException(String message) {
        super(message);
    }

    public RiskScoringException(String message, Throwable cause) {
        super(message, cause);
    }
}
