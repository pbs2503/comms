package com.bspark.comms.core.protocol.validation;

import lombok.Getter;

@Getter
public class CrcValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private CrcValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static CrcValidationResult valid() {
        return new CrcValidationResult(true, null);
    }

    public static CrcValidationResult invalid(String errorMessage) {
        return new CrcValidationResult(false, errorMessage);
    }

    @Override
    public String toString() {
        return valid ? "CRC Valid" : "CRC Invalid: " + errorMessage;
    }
}