package com.bspark.comms.events.connection;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.net.Socket;

@Getter
public class ConnectionEstablishedEvent extends ApplicationEvent {
    private final String clientId;
    private final Socket socket;
    private final String remoteAddress;

    public ConnectionEstablishedEvent(Object source, String clientId, Socket socket) {
        super(source);
        this.clientId = clientId;
        this.socket = socket;
        this.remoteAddress = socket != null ? socket.getInetAddress().getHostAddress() : "unknown";
    }

    // 편의 생성자 (Socket 없이 사용할 때)
    public ConnectionEstablishedEvent(Object source, String clientId, String remoteAddress) {
        super(source);
        this.clientId = clientId;
        this.socket = null;
        this.remoteAddress = remoteAddress;
    }
}