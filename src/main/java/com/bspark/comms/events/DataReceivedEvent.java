package com.bspark.comms.events;

import com.bspark.comms.data.MessageType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DataReceivedEvent extends ApplicationEvent {
    private final String clientId;
    private final MessageType messageType;
    private final byte[] data;

    public DataReceivedEvent(Object source, String clientId, MessageType messageType, byte[] data) {
        super(source);
        this.clientId = clientId;
        this.messageType = messageType;
        this.data = data;
    }

}