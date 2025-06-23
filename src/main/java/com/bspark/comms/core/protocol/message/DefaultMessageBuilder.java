package com.bspark.comms.core.protocol.message;

import com.bspark.comms.util.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class DefaultMessageBuilder implements MessageBuilder {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageBuilder.class);

    // 지원하는 opcode 목록
    private static final Set<Byte> SUPPORTED_OPCODES = Set.of(
            (byte) 0x11, // NETWORK_TEST
            (byte) 0x12, // STATUS_REQUEST
            (byte) 0x13, // STARTUP_CODE
            (byte) 0x21, // INTERSECTION_STATUS
            (byte) 0x22, // DETECTOR_INFO
            (byte) 0x23, // PHASE_INFO
            (byte) 0x24  // USER_REQUEST
    );

    private static final Map<Byte, byte[]> REQ_BYTE_ARRAY_MAP = Map.of(
            (byte)0x12, new byte[]{ 0x7f, 0x7f, 0x00, 0x08, 0x00, 0x01, 0x00, 0x12, 0x3B, 0x1F},
            (byte)0xA2, new byte[] {0x7F, 0x7F, 0x00, 0x08, 0x00, 0x01, 0x00, (byte)0xA2, (byte)0x8E, (byte)0x94}
    );

    @Override
    public byte[] buildMessage(byte opcode) {
        if (!isOpcodeSupported(opcode)) {
            throw new IllegalArgumentException("Unsupported opcode: " +
                    MessageUtils.formatOpcode(opcode));
        }

        // 기본 메시지 구조: [길이(2바이트)] + [opcode(1바이트)]
        byte[] message = REQ_BYTE_ARRAY_MAP.get(opcode);

        logger.debug("Built message for opcode {}: {}",
                MessageUtils.formatOpcode(opcode), MessageUtils.bytesToHex(message));

        return message;
    }


    @Override
    public byte[] buildMessage(byte opcode, byte[] data) {
        if (!isOpcodeSupported(opcode)) {
            throw new IllegalArgumentException("Unsupported opcode: " +
                    MessageUtils.formatOpcode(opcode));
        }

        if (data == null) {
            return buildMessage(opcode);
        }

        // 메시지 구조: [길이(2바이트)] + [opcode(1바이트)] + [데이터]
        int totalLength = 1 + data.length; // opcode + data
        byte[] message = new byte[2 + totalLength];

        // 길이 설정 (빅엔디안)
        message[0] = (byte) ((totalLength >> 8) & 0xFF);
        message[1] = (byte) (totalLength & 0xFF);

        // opcode 설정
        message[2] = opcode;

        // 데이터 복사
        System.arraycopy(data, 0, message, 3, data.length);

        logger.debug("Built message for opcode {} with {} bytes data: {}",
                MessageUtils.formatOpcode(opcode), data.length, MessageUtils.bytesToHex(message));

        return message;
    }

    @Override
    public boolean isOpcodeSupported(byte opcode) {
        return SUPPORTED_OPCODES.contains(opcode);
    }

    @Override
    public Set<Byte> getSupportedOpcodes() {
        return Set.copyOf(SUPPORTED_OPCODES);
    }
}