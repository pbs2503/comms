package com.bspark.comms.core.protocol.message;

import com.bspark.comms.core.protocol.validation.MessageValidator;
import com.bspark.comms.core.protocol.validation.ValidationResult;
import com.bspark.comms.data.Message;
import com.bspark.comms.data.MessageType;
import com.bspark.comms.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private final MessageValidator messageValidator;

    /**
     * 원시 메시지를 처리하여 Message 객체로 변환
     */
    public Message processRawMessage(String clientId, byte[] rawData) {
        if (!MessageUtils.isValidClientId(clientId)) {
            logger.warn("Invalid client ID provided: {}", clientId);
            return null;
        }

        // 메시지 검증
        ValidationResult validationResult = messageValidator.validateMessage(clientId, rawData);
        if (!validationResult.isValid()) {
            logger.warn("Message validation failed for client {}: {}", clientId, validationResult);
            return null;
        }

        // 메시지 파싱
        try {
            byte[] processedData = MessageUtils.convertToUnsigned(rawData);
            byte opcode = MessageUtils.extractOpcode(processedData);

            // 안전한 MessageType 분류
            MessageType messageType = classifyMessageType(opcode);

            Message message = Message.builder()
                    .clientId(clientId)
                    .opcode(opcode)
                    .data(processedData)
                    .timestamp(System.currentTimeMillis())
                    .type(messageType)
                    .build();

            logger.debug("Message processed successfully: {}", message);
            return message;

        } catch (Exception e) {
            logger.error("Error processing message from client {}: {}", clientId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * opcode를 기반으로 메시지 타입 분류 (안전한 방식)
     */
    private MessageType classifyMessageType(byte opcode) {
        try {
            // MessageType.fromOpcode() 호출
            return MessageType.fromOpcode(opcode);
        } catch (Exception e) {
            logger.warn("Failed to classify message type for opcode 0x{:02X}, using fallback: {}",
                    opcode & 0xFF, e.getMessage());

            // 폴백 분류 로직
            return fallbackClassifyMessageType(opcode);
        }
    }

    /**
     * 폴백 메시지 타입 분류
     */
    private MessageType fallbackClassifyMessageType(byte opcode) {
        return switch (opcode) {
            case 0x13 -> MessageType.INTERSECTION_STATUS;
            case 0x23 -> MessageType.DETECTOR_INFO;
            case 0x33 -> MessageType.PHASE_INFO;
            case 0x12 -> MessageType.STATUS_REQUEST;
            case (byte) 0xDA -> MessageType.NETWORK_TEST;
            case (byte) 0xA2 -> MessageType.STARTUP_CODE;
            default -> MessageType.USER_REQUEST;
        };
    }

    /**
     * 메시지 구조 분석
     */
    public MessageStructure analyzeMessageStructure(byte[] data) {
        if (!MessageUtils.isValidMessage(data)) {
            return null;
        }

        try {
            byte opcode = data.length > 6 ? MessageUtils.extractOpcode(data) : (byte) 0;

            return MessageStructure.builder()
                    .totalLength(data.length)
                    .declaredLength(MessageUtils.extractLength(data))
                    .opcode(opcode)
                    .opcodeAsHex(MessageUtils.formatOpcode(opcode))
                    .payloadLength(data.length > 7 ? data.length - 7 : 0)
                    .isValidStructure(MessageUtils.isValidMessageStructure(data))
                    .messageType(classifyMessageType(opcode))
                    .build();
        } catch (Exception e) {
            logger.warn("Failed to analyze message structure: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 메시지 구조 정보 (확장된 버전)
     */
    @lombok.Getter
    @lombok.Builder
    public static class MessageStructure {
        private final int totalLength;
        private final int declaredLength;
        private final byte opcode;
        private final String opcodeAsHex;
        private final int payloadLength;
        private final boolean isValidStructure;
        private final MessageType messageType;

        @Override
        public String toString() {
            return String.format("MessageStructure{total=%d, declared=%d, opcode=%s, type=%s, payload=%d, valid=%s}",
                    totalLength, declaredLength, opcodeAsHex, messageType, payloadLength, isValidStructure);
        }
    }
}