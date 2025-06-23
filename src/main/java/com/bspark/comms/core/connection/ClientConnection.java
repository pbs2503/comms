package com.bspark.comms.core.connection;

import lombok.Builder;
import lombok.Getter;

import java.net.Socket;

@Getter
@Builder
public class ClientConnection {
    private final String clientId;
    private final Socket socket;
    private final long connectedAt;
    private volatile long lastActivityAt;
    private volatile boolean active;

    /**
     * 활동 시간 업데이트
     */
    public void updateLastActivity() {
        this.lastActivityAt = System.currentTimeMillis();
    }

    /**
     * 연결을 비활성으로 표시
     */
    public void markAsInactive() {
        this.active = false;
    }

    /**
     * 소켓 유효성 확인
     */
    public boolean isSocketValid() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * 연결 지속 시간 계산 (밀리초)
     */
    public long getConnectionDuration() {
        return System.currentTimeMillis() - connectedAt;
    }

    /**
     * 마지막 활동 이후 경과 시간 (밀리초)
     */
    public long getTimeSinceLastActivity() {
        return System.currentTimeMillis() - lastActivityAt;
    }

    @Override
    public String toString() {
        return String.format("ClientConnection{id='%s', active=%s, connected=%s, duration=%dms}",
                clientId, active, isSocketValid(), getConnectionDuration());
    }
}