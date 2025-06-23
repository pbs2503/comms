package com.bspark.comms.core.protocol.message;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageTemplate {
    private final byte opcode;
    private final byte[] data;
    private final String description;
    private final int expectedLength;

    /**
     * 템플릿 데이터의 복사본 반환
     */
    public byte[] getDataCopy() {
        return data.clone();
    }

    /**
     * opcode를 16진수 문자열로 반환
     */
    public String getOpcodeAsHex() {
        return String.format("0x%02X", opcode & 0xFF);
    }

    @Override
    public String toString() {
        return String.format("MessageTemplate{opcode=%s, description='%s', length=%d}",
                getOpcodeAsHex(), description, data.length);
    }
}