package com.bspark.comms.events.connection;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ConnectionTerminatedEvent extends ApplicationEvent {
    private final String clientId;
    private final String remoteAddress;
    private final String reason;

    public ConnectionTerminatedEvent(String clientId, String remoteAddress, String reason) {
        super(clientId);
        this.clientId = clientId;
        this.remoteAddress = remoteAddress;
        this.reason = reason;
    }

    // 편의 생성자 (reason 기본값)
    public ConnectionTerminatedEvent(String clientId, String remoteAddress) {
        this(clientId, remoteAddress, "Connection closed");
    }
}