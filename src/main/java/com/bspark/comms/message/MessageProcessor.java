package com.bspark.comms.message;

import com.bspark.comms.data.MessageType;

public interface MessageProcessor {
    /**
     * 수신된 메시지를 처리하고 응답을 반환
     *
     * @param clientId 클라이언트 ID
     * @param messageType 메시지 유형
     * @param data 수신된 데이터
     * @return 응답 데이터 (응답이 없는 경우 null 또는 빈 배열)
     */
    byte[] processMessage(String clientId, MessageType messageType, byte[] data);
}