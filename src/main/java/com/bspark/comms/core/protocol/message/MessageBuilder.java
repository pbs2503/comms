package com.bspark.comms.core.protocol.message;

import java.util.Set;

public interface MessageBuilder {

    /**
     * opcode 기반 메시지 생성
     */
    byte[] buildMessage(byte opcode);

    /**
     * opcode와 데이터로 메시지 생성
     */
    byte[] buildMessage(byte opcode, byte[] data);

    /**
     * opcode 지원 여부 확인
     */
    boolean isOpcodeSupported(byte opcode);

    /**
     * 지원하는 opcode 목록 반환
     */
    Set<Byte> getSupportedOpcodes();
}