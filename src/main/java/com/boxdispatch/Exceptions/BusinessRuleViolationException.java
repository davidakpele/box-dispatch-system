package com.boxdispatch.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BusinessRuleViolationException extends RuntimeException {
    private final String errorCode;

    public BusinessRuleViolationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static BusinessRuleViolationException weightLimitExceeded(double available, double requested) {
        return new BusinessRuleViolationException(
            String.format("Weight limit exceeded. Available: %.3fg, Requested: %.3fg", available, requested),
            "WEIGHT_LIMIT_EXCEEDED"
        );
    }

    public static BusinessRuleViolationException lowBattery(int battery) {
        return new BusinessRuleViolationException(
            String.format("Box battery too low for loading: %d%%. Minimum required: 25%%", battery),
            "BATTERY_TOO_LOW"
        );
    }

    public static BusinessRuleViolationException invalidStateTransition(Object from, Object to) {
        return new BusinessRuleViolationException(
            String.format("Invalid state transition from %s to %s", from, to),
            "INVALID_STATE_TRANSITION"
        );
    }

    public static BusinessRuleViolationException duplicateItemCode(String code) {
        return new BusinessRuleViolationException(
            "Item with code '" + code + "' already exists",
            "DUPLICATE_ITEM_CODE"
        );
    }
}