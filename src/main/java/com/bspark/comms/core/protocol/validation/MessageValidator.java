package com.bspark.comms.core.protocol.validation;

import com.bspark.comms.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageValidator {
    private static final Logger logger = LoggerFactory.getLogger(MessageValidator.class);

    private final CrcValidator crcValidator;

    /**
     * 메시지 유효성 검증
     */
    public ValidationResult validateMessage(String clientId, byte[] data) {
        // 기본 유효성 검사
        if (!MessageUtils.isValidClientId(clientId)) {
            return ValidationResult.invalid("Invalid client ID");
        }

        if (!MessageUtils.isValidMessage(data)) {
            return ValidationResult.invalid("Invalid message data");
        }

        // CRC 검증
        CrcValidationResult crcResult = crcValidator.validate(data);
        if (!crcResult.isValid()) {
            return ValidationResult.invalid("CRC validation failed: " + crcResult.getErrorMessage());
        }

        // 메시지 구조 검증
        ValidationResult structureResult = validateMessageStructure(data);
        if (!structureResult.isValid()) {
            return structureResult;
        }

        logger.debug("Message validation passed for client: {}", clientId);
        return ValidationResult.valid();
    }

    /**
     * 메시지 구조 검증
     */
    private ValidationResult validateMessageStructure(byte[] data) {
        try {
            // 길이 필드 검증
            if (data.length < 2) {
                return ValidationResult.invalid("Message too short for length field");
            }

            int declaredLength = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF) + 2; //  + 2: CRC 포함
            if (declaredLength != data.length) {
                return ValidationResult.invalid(
                        String.format("Length mismatch: declared=%d, actual=%d", declaredLength, data.length));
            }

            // opcode 위치 검증
            if (data.length < 8) {
                return ValidationResult.invalid("Message too short for opcode");
            }

            return ValidationResult.valid();

        } catch (Exception e) {
            return ValidationResult.invalid("Structure validation error: " + e.getMessage());
        }
    }
}