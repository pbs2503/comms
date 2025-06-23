package com.bspark.comms.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.net.Socket;

public class DataHandlerRegistrationEvent extends ApplicationEvent {

    @Getter
    private final String clientId;
    @Getter
    private final Socket socket;

    public DataHandlerRegistrationEvent(Object source, String clientId, Socket socket) {
        super(source);
        this.clientId = clientId;
        this.socket = socket;
    }
}
