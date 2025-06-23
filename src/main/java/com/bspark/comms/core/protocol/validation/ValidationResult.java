package com.bspark.comms.core.protocol.validation;

import lombok.Getter;

@Getter
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }

    @Override
    public String toString() {
        return valid ? "Valid" : "Invalid: " + errorMessage;
    }
}