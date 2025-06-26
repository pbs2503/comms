package com.bspark.comms.message;

import com.bspark.comms.data.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultMessageProcessor implements MessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageProcessor.class);

    @Override
    public byte[] processMessage(String clientId, MessageType messageType, byte[] data) {
        logger.debug("메시지 처리: 클라이언트={}, 유형={}, 크기={}", clientId, messageType, data.length);

        // 여기에 메시지 처리 로직 구현
        // 현재는 기본 응답만 생성

        if (messageType.isResponse()) {
            // 응답 메시지 처리 (교차로 상태, 검지기 정보 등)
            return createAcknowledgement(data);
        } else if (messageType.isRequest()) {
            // 요청 메시지 처리 (상태 요청, 네트워크 테스트 등)
            return createCommandResponse(messageType, data);
        } else {
            logger.warn("지원되지 않는 메시지 유형: {}", messageType);
            return null;
        }
    }

    /**
     * 간단한 ACK 메시지 생성
     */
    private byte[] createAcknowledgement(byte[] originalData) {
        // 단순 예시로 'ACK' + 데이터 길이를 응답
        String response = String.format("ACK:%d", originalData.length);
        return response.getBytes();
    }

    /**
     * 명령 응답 메시지 생성
     */
    private byte[] createCommandResponse(MessageType messageType, byte[] originalData) {
        if (messageType == MessageType.NETWORK_TEST) {
            return "NETWORK_TEST_ACK".getBytes();
        } else if (messageType == MessageType.STATUS_REQUEST) {
            return "STATUS_ACK".getBytes();
        } else {
            return String.format("CMD_ACK:%s", messageType.name()).getBytes();
        }
    }
}