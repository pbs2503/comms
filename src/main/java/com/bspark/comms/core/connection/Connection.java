
package com.bspark.comms.core.connection;

import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Builder
public class Connection {

    private static final Logger logger = LoggerFactory.getLogger(Connection.class);

    private final String clientId;
    private final Socket socket;
    private final long connectedAt;

    private final AtomicLong lastActivityAt;
    private final AtomicBoolean active;

    // 통계 정보
    private final AtomicLong bytesReceived;
    private final AtomicLong bytesSent;
    private final AtomicLong messageCount;

    public Connection(String clientId, Socket socket, long connectedAt,
                      AtomicLong lastActivityAt, AtomicBoolean active,
                      AtomicLong bytesReceived, AtomicLong bytesSent, AtomicLong messageCount) {
        this.clientId = clientId;
        this.socket = socket;
        this.connectedAt = connectedAt;
        this.lastActivityAt = lastActivityAt != null ? lastActivityAt : new AtomicLong(System.currentTimeMillis());
        this.active = active != null ? active : new AtomicBoolean(true);
        this.bytesReceived = bytesReceived != null ? bytesReceived : new AtomicLong(0);
        this.bytesSent = bytesSent != null ? bytesSent : new AtomicLong(0);
        this.messageCount = messageCount != null ? messageCount : new AtomicLong(0);
    }

    /**
     * 마지막 활동 시간 업데이트
     */
    public void updateLastActivity() {
        lastActivityAt.set(System.currentTimeMillis());
    }

    /**
     * 연결 활성 상태 확인
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * 소켓 유효성 확인
     */
    public boolean isSocketValid() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * 비활성 시간 (밀리초)
     */
    public long getInactivityDurationMillis() {
        return System.currentTimeMillis() - lastActivityAt.get();
    }

    /**
     * 연결 지속 시간 (밀리초)
     */
    public long getConnectionDurationMillis() {
        return System.currentTimeMillis() - connectedAt;
    }

    /**
     * 바이트 수신 통계 업데이트
     */
    public void addBytesReceived(long bytes) {
        bytesReceived.addAndGet(bytes);
        updateLastActivity();
    }

    /**
     * 바이트 전송 통계 업데이트
     */
    public void addBytesSent(long bytes) {
        bytesSent.addAndGet(bytes);
        updateLastActivity();
    }

    /**
     * 메시지 카운트 증가
     */
    public void incrementMessageCount() {
        messageCount.incrementAndGet();
    }

    /**
     * 연결 비활성화
     */
    public void deactivate() {
        active.set(false);
    }

    /**
     * 연결 종료
     */
    public void close() {
        deactivate();

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                logger.debug("Connection closed for client: {}", clientId);
            } catch (IOException e) {
                logger.warn("Error closing connection for client {}: {}", clientId, e.getMessage());
            }
        }
    }

    /**
     * 연결 상태 정보 반환
     */
    public ConnectionStatus getStatus() {
        return ConnectionStatus.builder()
                .clientId(clientId)
                .active(isActive())
                .socketValid(isSocketValid())
                .connectedAt(connectedAt)
                .lastActivityAt(lastActivityAt.get())
                .inactivityDuration(getInactivityDurationMillis())
                .connectionDuration(getConnectionDurationMillis())
                .bytesReceived(bytesReceived.get())
                .bytesSent(bytesSent.get())
                .messageCount(messageCount.get())
                .build();
    }

    /**
     * 클라이언트 IP 주소 반환
     */
    public String getClientIp() {
        return socket != null ? socket.getInetAddress().getHostAddress() : "unknown";
    }

    /**
     * 클라이언트 포트 반환
     */
    public int getClientPort() {
        return socket != null ? socket.getPort() : 0;
    }

    /**
     * 로컬 포트 반환
     */
    public int getLocalPort() {
        return socket != null ? socket.getLocalPort() : 0;
    }

    @Override
    public String toString() {
        return String.format("Connection{clientId='%s', active=%s, connected=%s, duration=%dms, bytes=%d/%d}",
                clientId, isActive(), isSocketValid(), getConnectionDurationMillis(),
                bytesReceived.get(), bytesSent.get());
    }

    /**
     * 연결 상태 정보 클래스
     */
    @Getter
    @Builder
    public static class ConnectionStatus {
        private final String clientId;
        private final boolean active;
        private final boolean socketValid;
        private final long connectedAt;
        private final long lastActivityAt;
        private final long inactivityDuration;
        private final long connectionDuration;
        private final long bytesReceived;
        private final long bytesSent;
        private final long messageCount;

        @Override
        public String toString() {
            return String.format("Status{active=%s, valid=%s, duration=%dms, inactive=%dms, msgs=%d}",
                    active, socketValid, connectionDuration, inactivityDuration, messageCount);
        }
    }
}