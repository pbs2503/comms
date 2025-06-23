package com.bspark.comms.network.transport;

import com.bspark.comms.core.connection.ConnectionManager;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    private final ConnectionManager connectionManager;
    private final ExecutorService senderExecutor = Executors.newFixedThreadPool(8,
            r -> {
                Thread t = new Thread(r, "message-sender");
                t.setDaemon(true);
                return t;
            });

    /**
     * 메시지 전송 (동기)
     */
    public void sendMessage(String clientId, byte[] data) {
        if (data == null || data.length == 0) {
            logger.warn("Attempted to send empty message to client: {}", clientId);
            return;
        }

        senderExecutor.submit(() -> doSendMessage(clientId, data));
    }

    /**
     * 메시지 전송 (비동기)
     */
    public CompletableFuture<Void> sendMessageAsync(String clientId, byte[] data) {
        return CompletableFuture.runAsync(() -> doSendMessage(clientId, data), senderExecutor);
    }

    /**
     * 실제 메시지 전송 로직
     */
    private void doSendMessage(String clientId, byte[] data) {
        if (!connectionManager.isActive(clientId)) {
            logger.debug("Skipping message send to inactive client: {}", clientId);
            return;
        }

        Socket socket = connectionManager.getSocket(clientId);
        if (socket == null || socket.isClosed()) {
            logger.warn("Cannot send message to client {}: socket not available", clientId);
            connectionManager.removeConnection(clientId);
            return;
        }

        try {
            synchronized (socket) { // 동일 소켓에 대한 동시 쓰기 방지
                OutputStream out = socket.getOutputStream();
                out.write(data);
                out.flush();
            }

            connectionManager.updateActivity(clientId);
            //logger.debug("Message sent to client: {}, data: {}", clientId, HexUtils.toHexString(data));

        } catch (IOException e) {
            logger.error("Error sending message to client {}: {}", clientId, e.getMessage());
            // 에러 발생 시 소켓 강제 종료 추가
            forceCloseSocket(clientId);
            connectionManager.removeConnection(clientId);
            throw new MessageSendException("Failed to send message to client: " + clientId, e);
        }
    }

    /**
     * 소켓 강제 종료
     */
    private void forceCloseSocket(String clientId) {
        Socket socket = connectionManager.getSocket(clientId);
        if (socket != null && !socket.isClosed()) {
            try {
                socket.shutdownOutput();
                socket.shutdownInput();
                socket.close();
                logger.debug("Force closed socket for client: {}", clientId);
            } catch (IOException e) {
                logger.warn("Error force closing socket for client {}: {}", clientId, e.getMessage());
            }
        }
    }

    /**
     * 모든 활성 클라이언트에게 메시지 전송
     */
    public void sendMessageToAllActiveClients(byte[] data) {
        if (connectionManager.getActiveConnectionCount() == 0) {
            logger.debug("No active clients to send message to");
            return;
        }

        var activeClients = connectionManager.getActiveClientIds();
        logger.debug("Broadcasting message to {} active clients", activeClients.size());

        for (String clientId : activeClients) {
            sendMessage(clientId, data);
        }
    }

    /**
     * 특정 클라이언트들에게 메시지 전송
     */
    public void sendMessageToClients(java.util.Set<String> clientIds, byte[] data) {
        if (clientIds == null || clientIds.isEmpty()) {
            logger.debug("No client IDs specified for message sending");
            return;
        }

        logger.debug("Sending message to {} specified clients", clientIds.size());

        for (String clientId : clientIds) {
            if (connectionManager.isActive(clientId)) {
                sendMessage(clientId, data);
            } else {
                logger.debug("Skipping inactive client: {}", clientId);
            }
        }
    }

    /**
     * 메시지 전송 가능 여부 확인
     */
    public boolean canSendMessage(String clientId) {
        return connectionManager.canSendMessage(clientId);
    }

    /**
     * 전송 통계 정보
     */
    public SendingStatistics getSendingStatistics() {
        return SendingStatistics.builder()
                .activeConnections(connectionManager.getActiveConnectionCount())
                .totalConnections(connectionManager.getTotalConnectionCount())
                .queuedTasks(((java.util.concurrent.ThreadPoolExecutor) senderExecutor).getQueue().size())
                .completedTasks(((java.util.concurrent.ThreadPoolExecutor) senderExecutor).getCompletedTaskCount())
                .build();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down MessageSender...");
        senderExecutor.shutdown();
        try {
            if (!senderExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                senderExecutor.shutdownNow();
                if (!senderExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("MessageSender did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            senderExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("MessageSender shutdown completed");
    }

    /**
     * 전송 통계 정보
     */
    @lombok.Getter
    @lombok.Builder
    public static class SendingStatistics {
        private final int activeConnections;
        private final int totalConnections;
        private final int queuedTasks;
        private final long completedTasks;

        @Override
        public String toString() {
            return String.format("SendingStatistics{active=%d, total=%d, queued=%d, completed=%d}",
                    activeConnections, totalConnections, queuedTasks, completedTasks);
        }
    }
}