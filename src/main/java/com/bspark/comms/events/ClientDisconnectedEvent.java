package com.bspark.comms.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClientDisconnectedEvent extends ApplicationEvent {
    private final String clientId;

    public ClientDisconnectedEvent(Object source, String clientId) {
        super(source);
        this.clientId = clientId;
    }

}