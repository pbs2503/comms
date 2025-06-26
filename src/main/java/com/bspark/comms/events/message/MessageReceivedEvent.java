package com.bspark.comms.events.message;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MessageReceivedEvent extends ApplicationEvent {
    private final String clientId;
    private final byte[] data;
    // ApplicationEvent의 getTimestamp()와 충돌하지 않도록 다른 이름 사용
    private final long receivedTimestamp; // timestamp와 다른 이름 사용

    public MessageReceivedEvent(String clientId, byte[] data, long receivedTimestamp) {
        super(clientId);
        this.clientId = clientId;
        this.data = data != null ? data.clone() : new byte[0];
        this.receivedTimestamp = receivedTimestamp;
    }

    // 편의 생성자 (timestamp 자동 설정)
    public MessageReceivedEvent(String clientId, byte[] data) {
        this(clientId, data, System.currentTimeMillis());
    }

    public int getDataLength() {
        return data != null ? data.length : 0;
    }

}