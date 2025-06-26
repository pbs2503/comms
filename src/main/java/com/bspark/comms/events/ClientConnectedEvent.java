package com.bspark.comms.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientConnectedEvent extends ApplicationEvent {
    private final String clientId;
    private final String clientIp;

    public ClientConnectedEvent(Object source, String clientId, String clientIp) {
        super(source);
        this.clientId = clientId;
        this.clientIp = clientIp;
    }

}