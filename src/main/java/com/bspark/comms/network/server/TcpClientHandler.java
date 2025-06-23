package com.bspark.comms.network.server;

import com.bspark.comms.core.connection.ConnectionManager;
import com.bspark.comms.event.connection.ConnectionEstablishedEvent;
import com.bspark.comms.event.connection.ConnectionTerminatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TcpClientHandler {

    private static final Logger logger = LoggerFactory.getLogger(TcpClientHandler.class);

    private final ApplicationEventPublisher eventPublisher;
    private final ConnectionManager connectionManager;

    @EventListener
    public void handleConnectionEstablished(ConnectionEstablishedEvent event) {
        try {
            connectionManager.addConnection(event.getClientId(), event.getSocket());
            logger.info("Client {} connected and registered", event.getClientId());
        } catch (Exception e) {
            logger.error("Error handling connection establishment for {}: {}",
                    event.getClientId(), e.getMessage(), e);
        }
    }

    @EventListener
    public void handleConnectionTerminated(ConnectionTerminatedEvent event) {
        try {
            connectionManager.removeConnection(event.getClientId());
            logger.info("Client {} disconnected and removed", event.getClientId());
        } catch (Exception e) {
            logger.error("Error handling connection termination for {}: {}",
                    event.getClientId(), e.getMessage(), e);
        }
    }

    /**
     * 클라이언트 연결 상태 확인
     */
    public boolean isClientConnected(String clientId) {
        return connectionManager.isActive(clientId);
    }

    /**
     * 활성 연결 수 반환
     */
    public int getActiveConnectionCount() {
        return connectionManager.getActiveConnectionCount();
    }

    /**
     * 활성 클라이언트 ID 목록 반환
     */
    public java.util.Set<String> getActiveClientIds() {
        return connectionManager.getActiveClientIds();
    }

    /**
     * 연결 통계 정보 반환
     */
    public com.bspark.comms.core.connection.ConnectionStatistics getConnectionStatistics() {
        return connectionManager.getStatistics();
    }
}