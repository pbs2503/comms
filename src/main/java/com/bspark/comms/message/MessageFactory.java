package com.bspark.comms.message;

import com.bspark.comms.data.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component
public class MessageFactory {
    private static final Logger logger = LoggerFactory.getLogger(MessageFactory.class);

    /**
     * 메시지 생성 (opcode + 데이터)
     */
    public byte[] buildMessage(int opcode, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(1 + (data != null ? data.length : 0));
        buffer.put((byte) opcode);
        if (data != null && data.length > 0) {
            buffer.put(data);
        }
        return buffer.array();
    }

    /**
     * 상태 요청 메시지 생성
     */
    public byte[] createStatusRequestMessage() {
        ByteBuffer dataBuffer = ByteBuffer.allocate(10);

        dataBuffer.put((byte)0x7F);
        dataBuffer.put((byte)0x7F);
        dataBuffer.put((byte)0x00);
        dataBuffer.put((byte)0x08);
        dataBuffer.put((byte)0x00);
        dataBuffer.put((byte)0x01);
        dataBuffer.put((byte)0x00);
        dataBuffer.put((byte)0x12);
        dataBuffer.put((byte)0x3B);
        dataBuffer.put((byte)0x1F);

        return dataBuffer.array();
    }

    /**
     * 하트비트 메시지 생성
     */
    public byte[] createHeartbeatMessage() {
        return buildMessage(MessageType.NETWORK_TEST.getOpcode(), new byte[0]);
    }
}