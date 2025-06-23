package com.bspark.comms.core.protocol.message;

import com.bspark.comms.util.MessageUtils;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Builder
public class Message {
    private final String clientId;
    private final byte opcode;
    private final byte[] data;
    private final long timestamp;
    private final MessageType type;

    /**
     * 메시지의 타임스탬프를 LocalDateTime으로 반환
     */
    public LocalDateTime getTimestampAsDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * opcode를 16진수 문자열로 반환
     */
    public String getOpcodeAsHex() {
        return MessageUtils.formatOpcode(opcode);
    }

    /**
     * 데이터를 16진수 문자열로 반환
     */
    public String getDataAsHex() {
        return MessageUtils.bytesToHex(data);
    }

    /**
     * 메시지 크기 반환
     */
    public int getSize() {
        return data != null ? data.length : 0;
    }

    /**
     * 메시지 유효성 검증
     */
    public boolean isValid() {
        return MessageUtils.isValidClientId(clientId) &&
                MessageUtils.isValidMessage(data) &&
                timestamp > 0;
    }

    /**
     * 길이 필드 추출
     */
    public int getLength() {
        if (data == null || data.length < 2) {
            return 0;
        }
        return ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    }

    /**
     * 메시지가 응답인지 확인
     */
    public boolean isResponse() {
        return type == MessageType.INTERSECTION_STATUS ||
                type == MessageType.DETECTOR_INFO ||
                type == MessageType.PHASE_INFO ||
                type == MessageType.USER_REQUEST;
    }

    /**
     * 메시지가 요청인지 확인
     */
    public boolean isRequest() {
        return type == MessageType.STATUS_REQUEST ||
                type == MessageType.NETWORK_TEST ||
                type == MessageType.STARTUP_CODE;
    }

    @Override
    public String toString() {
        return String.format("Message{clientId='%s', type=%s, opcode=%s, size=%d, timestamp=%s}",
                clientId, type, getOpcodeAsHex(), getSize(), getTimestampAsDateTime());
    }
}