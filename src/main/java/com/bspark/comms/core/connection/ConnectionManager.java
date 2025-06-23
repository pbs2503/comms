package com.bspark.comms.core.connection;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnectionCount = new AtomicInteger(0);

    /**
     * 새로운 연결 추가
     */
    public void addConnection(String clientId, Socket socket) {
        if (clientId == null || socket == null) {
            throw new IllegalArgumentException("ClientId and socket cannot be null");
        }

        long currentTime = System.currentTimeMillis();

        Connection connection = Connection.builder()
                .clientId(clientId)
                .socket(socket)
                .connectedAt(currentTime)
                .lastActivityAt(new AtomicLong(currentTime))
                .active(new AtomicBoolean(true))
                .bytesReceived(new AtomicLong(0))
                .bytesSent(new AtomicLong(0))
                .messageCount(new AtomicLong(0))
                .build();

        connections.put(clientId, connection);
        totalConnectionCount.incrementAndGet();

        logger.debug("Connection added for client: {}", clientId);
    }

    /**
     * 연결 제거
     */
    public void removeConnection(String clientId) {
        Connection connection = connections.remove(clientId);
        if (connection != null) {
            connection.close();
            logger.debug("Connection removed for client: {}", clientId);
        }
    }

    /**
     * 연결 활동 시간 업데이트
     */
    public void updateActivity(String clientId) {
        Connection connection = connections.get(clientId);
        if (connection != null && connection.isActive()) {
            connection.updateLastActivity();
        }
    }

    /**
     * 클라이언트가 활성 상태인지 확인
     */
    public boolean isActive(String clientId) {
        Connection connection = connections.get(clientId);
        return connection != null && connection.isActive() && connection.isSocketValid();
    }

    /**
     * 특정 클라이언트의 소켓 반환
     */
    public Socket getSocket(String clientId) {
        Connection connection = connections.get(clientId);
        return connection != null && connection.isActive() ? connection.getSocket() : null;
    }

    /**
     * 활성 연결 수 반환
     */
    public int getActiveConnectionCount() {
        return (int) connections.values().stream()
                .filter(Connection::isActive)
                .filter(Connection::isSocketValid)
                .count();
    }

    /**
     * 전체 연결 수 반환 (히스토리 포함)
     */
    public int getTotalConnectionCount() {
        return totalConnectionCount.get();
    }

    /**
     * 현재 연결된 클라이언트 수 반환
     */
    public int getCurrentConnectionCount() {
        return connections.size();
    }

    /**
     * 활성 클라이언트 ID 목록 반환
     */
    public Set<String> getActiveClientIds() {
        return connections.entrySet().stream()
                .filter(entry -> entry.getValue().isActive())
                .filter(entry -> entry.getValue().isSocketValid())
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 특정 클라이언트의 연결 정보 반환
     */
    public Connection getConnection(String clientId) {
        return connections.get(clientId);
    }

    /**
     * 모든 연결 종료
     */
    public void closeAllConnections() {
        logger.info("Closing all connections... Current count: {}", connections.size());

        connections.values().forEach(Connection::close);
        connections.clear();

        logger.info("All connections closed");
    }

    /**
     * 연결 통계 정보 반환
     */
    public ConnectionStatistics getStatistics() {
        int active = getActiveConnectionCount();
        int current = getCurrentConnectionCount();
        int total = getTotalConnectionCount();

        return ConnectionStatistics.builder()
                .activeConnections(active)
                .currentConnections(current)
                .totalConnections(total)
                .connectionSuccessRate(total > 0 ? (double) active / total * 100 : 0.0)
                .build();
    }

    /**
     * 비활성 연결 정리
     */
    public void cleanupInactiveConnections(long inactivityThresholdMillis) {
        long currentTime = System.currentTimeMillis();

        connections.entrySet().removeIf(entry -> {
            Connection connection = entry.getValue();
            boolean shouldRemove = !connection.isSocketValid() ||
                    (currentTime - connection.getLastActivityAt().get()) > inactivityThresholdMillis;

            if (shouldRemove) {
                connection.close();
                logger.debug("Removed inactive connection: {}", entry.getKey());
            }

            return shouldRemove;
        });
    }

    /**
     * 특정 클라이언트에게 메시지 전송 가능 여부 확인
     */
    public boolean canSendMessage(String clientId) {
        Socket socket = getSocket(clientId);
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * 연결된 모든 클라이언트의 상태 정보 반환
     */
    public java.util.Map<String, ConnectionStatus> getAllConnectionStatus() {
        return connections.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        entry -> {
                            Connection conn = entry.getValue();
                            return ConnectionStatus.builder()
                                    .active(conn.isActive())
                                    .socketValid(conn.isSocketValid())
                                    .connectedAt(conn.getConnectedAt())
                                    .lastActivityAt(conn.getLastActivityAt().get())
                                    .inactivityDuration(conn.getInactivityDurationMillis())
                                    .build();
                        }
                ));
    }

    /**
     * 특정 클라이언트의 연결 통계 업데이트
     */
    public void updateConnectionStats(String clientId, long bytesReceived, long bytesSent) {
        Connection connection = connections.get(clientId);
        if (connection != null) {
            if (bytesReceived > 0) {
                connection.addBytesReceived(bytesReceived);
            }
            if (bytesSent > 0) {
                connection.addBytesSent(bytesSent);
            }
            connection.incrementMessageCount();
        }
    }

    /**
     * 특정 클라이언트의 수신 바이트 수 업데이트
     */
    public void addBytesReceived(String clientId, long bytes) {
        Connection connection = connections.get(clientId);
        if (connection != null) {
            connection.addBytesReceived(bytes);
        }
    }

    /**
     * 특정 클라이언트의 전송 바이트 수 업데이트
     */
    public void addBytesSent(String clientId, long bytes) {
        Connection connection = connections.get(clientId);
        if (connection != null) {
            connection.addBytesSent(bytes);
        }
    }

    /**
     * 특정 클라이언트의 메시지 카운트 증가
     */
    public void incrementMessageCount(String clientId) {
        Connection connection = connections.get(clientId);
        if (connection != null) {
            connection.incrementMessageCount();
        }
    }

    /**
     * 연결 정리 (타임아웃 기반)
     */
    public int cleanupConnectionsByTimeout(long timeoutMillis) {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        for (java.util.Iterator<java.util.Map.Entry<String, Connection>> iterator = connections.entrySet().iterator(); iterator.hasNext();) {
            java.util.Map.Entry<String, Connection> entry = iterator.next();
            Connection connection = entry.getValue();

            boolean shouldRemove = !connection.isSocketValid() ||
                    (currentTime - connection.getLastActivityAt().get()) > timeoutMillis;

            if (shouldRemove) {
                connection.close();
                iterator.remove();
                removedCount++;
                logger.debug("Connection timeout removed: {}", entry.getKey());
            }
        }

        if (removedCount > 0) {
            logger.info("Cleaned up {} timed-out connections", removedCount);
        }

        return removedCount;
    }

    /**
     * 모든 연결의 상세 정보 로깅
     */
    public void logAllConnectionDetails() {
        logger.info("=== Connection Details ===");
        logger.info("Total connections: {}", connections.size());
        logger.info("Active connections: {}", getActiveConnectionCount());

        connections.forEach((clientId, connection) -> {
            logger.info("Client {}: {}", clientId, connection.toString());
        });
        logger.info("========================");
    }

    @PreDestroy
    public void cleanup() {
        logger.info("ConnectionManager cleanup starting...");
        closeAllConnections();
        logger.info("ConnectionManager cleanup completed");
    }

    /**
     * 연결 상태 정보
     */
    @lombok.Getter
    @lombok.Builder
    public static class ConnectionStatus {
        private final boolean active;
        private final boolean socketValid;
        private final long connectedAt;
        private final long lastActivityAt;
        private final long inactivityDuration;

        @Override
        public String toString() {
            return String.format("ConnectionStatus{active=%s, valid=%s, inactive=%dms}",
                    active, socketValid, inactivityDuration);
        }
    }
}