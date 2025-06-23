
package com.bspark.comms.service.communication;

import com.bspark.comms.core.connection.ConnectionManager;
import com.bspark.comms.core.connection.ConnectionStatistics;
import com.bspark.comms.core.protocol.message.MessageBuilder;
import com.bspark.comms.network.transport.MessageSender;
import com.bspark.comms.network.transport.MessageSendException;
import com.bspark.comms.service.monitoring.MessageTrackingService;
import com.bspark.comms.util.MessageUtils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class CommunicationService {
    private static final Logger logger = LoggerFactory.getLogger(CommunicationService.class);

    private final ConnectionManager connectionManager;
    private final MessageSender messageSender;
    private final MessageBuilder messageBuilder;
    private final MessageTrackingService trackingService;

    // 요청 목록 관리를 위한 캐시
    private final Set<String> requestClientList = ConcurrentHashMap.newKeySet();

    // 통계 카운터들
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalBroadcastsSent = new AtomicLong(0);
    private final AtomicLong totalGroupMessagesSent = new AtomicLong(0);

    // =================== 연결 관리 API ===================

    /**
     * 활성 클라이언트 목록 조회
     */
    public Set<String> getActiveClients() {
        return connectionManager.getActiveClientIds();
    }

    /**
     * 클라이언트 활성 상태 확인
     */
    public boolean isClientActive(String clientId) {
        if (!MessageUtils.isValidClientId(clientId)) {
            return false;
        }
        return connectionManager.isActive(clientId);
    }

    /**
     * 클라이언트 제거 및 히스토리 정리
     */
    public void removeClient(String clientId) {
        if (!MessageUtils.isValidClientId(clientId)) {
            logger.warn("Invalid client ID for removal: {}", clientId);
            return;
        }

        connectionManager.removeConnection(clientId);
        trackingService.clearHistory(clientId);
        requestClientList.remove(clientId);

        logger.info("Client {} removed and all history cleared", clientId);
    }

    /**
     * 비활성 클라이언트 정리
     */
    public int cleanupInactiveClients() {
        Set<String> allClients = Set.copyOf(requestClientList);
        int removedCount = 0;

        for (String clientId : allClients) {
            if (!connectionManager.isActive(clientId)) {
                removeClient(clientId);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            logger.info("Cleaned up {} inactive clients", removedCount);
        }

        return removedCount;
    }

    // =================== 메시지 전송 API ===================

    /**
     * 단일 클라이언트에게 opcode 기반 메시지 전송
     */
    public void sendMessage(String clientId, byte opcode) {
        validateClientForSend(clientId);
        validateOpcode(opcode);

        try {
            byte[] message = messageBuilder.buildMessage(opcode);
            messageSender.sendMessage(clientId, message);
            trackingService.recordTransmission(clientId, opcode);
            totalMessagesSent.incrementAndGet();

            logger.debug("Message sent to {}: opcode {}", clientId, MessageUtils.formatOpcode(opcode));

        } catch (Exception e) {
            logger.error("Failed to send message to {}: {}", clientId, e.getMessage());
            throw new MessageSendException("Message send failed for client: " + clientId, e);
        }
    }

    /**
     * 단일 클라이언트에게 직접 데이터 전송
     */
    public void sendMessage(String clientId, byte[] data) {
        validateClientForSend(clientId);
        validateMessageData(data);

        try {
            messageSender.sendMessage(clientId, data);
            totalMessagesSent.incrementAndGet();

            logger.debug("Custom message sent to {}: {} bytes", clientId, data.length);

        } catch (Exception e) {
            logger.error("Failed to send custom message to {}: {}", clientId, e.getMessage());
            throw new MessageSendException("Custom message send failed for client: " + clientId, e);
        }
    }

    /**
     * 모든 활성 클라이언트에게 브로드캐스트
     */
    public void broadcastMessage(byte opcode) {
        validateOpcode(opcode);

        Set<String> activeClients = connectionManager.getActiveClientIds();
        if (activeClients.isEmpty()) {
            logger.debug("No active clients for broadcast");
            return;
        }

        try {
            byte[] message = messageBuilder.buildMessage(opcode);
            int successCount = 0;

            for (String clientId : activeClients) {
                try {
                    messageSender.sendMessage(clientId, message);
                    trackingService.recordTransmission(clientId, opcode);
                    successCount++;
                } catch (Exception e) {
                    logger.warn("Failed to send broadcast to {}: {}", clientId, e.getMessage());
                }
            }

            totalBroadcastsSent.incrementAndGet();
            totalMessagesSent.addAndGet(successCount);

            logger.info("Broadcast sent to {}/{} clients: opcode {}",
                    successCount, activeClients.size(), MessageUtils.formatOpcode(opcode));

        } catch (Exception e) {
            logger.error("Broadcast message build failed: {}", e.getMessage());
            throw new MessageSendException("Broadcast failed", e);
        }
    }

    /**
     * 특정 클라이언트 그룹에게 메시지 전송
     */
    public void sendMessageToGroup(Set<String> clientIds, byte opcode) {
        validateOpcode(opcode);

        if (clientIds == null || clientIds.isEmpty()) {
            logger.warn("Empty client group provided");
            return;
        }

        try {
            byte[] message = messageBuilder.buildMessage(opcode);
            int successCount = 0;

            for (String clientId : clientIds) {
                if (connectionManager.isActive(clientId)) {
                    try {
                        messageSender.sendMessage(clientId, message);
                        trackingService.recordTransmission(clientId, opcode);
                        successCount++;
                    } catch (Exception e) {
                        logger.warn("Failed to send group message to {}: {}", clientId, e.getMessage());
                    }
                } else {
                    logger.debug("Skipping inactive client in group: {}", clientId);
                }
            }

            totalGroupMessagesSent.incrementAndGet();
            totalMessagesSent.addAndGet(successCount);

            logger.info("Group message sent to {}/{} clients: opcode {}",
                    successCount, clientIds.size(), MessageUtils.formatOpcode(opcode));

        } catch (Exception e) {
            logger.error("Group message build failed: {}", e.getMessage());
            throw new MessageSendException("Group message failed", e);
        }
    }

    // =================== 요청 목록 관리 ===================

    /**
     * 클라이언트를 요청 목록에 추가
     */
    public void addToRequestList(String clientId) {
        if (MessageUtils.isValidClientId(clientId)) {
            requestClientList.add(clientId);
            logger.debug("Client {} added to request list", clientId);
        }
    }

    /**
     * 클라이언트를 요청 목록에서 제거
     */
    public void removeFromRequestList(String clientId) {
        if (requestClientList.remove(clientId)) {
            logger.debug("Client {} removed from request list", clientId);
        }
    }

    /**
     * 요청 목록의 클라이언트 ID들 반환
     */
    public Set<String> getRequestClientIds() {
        return Set.copyOf(requestClientList);
    }

    // =================== 통계 및 모니터링 API ===================

    /**
     * 연결 통계 조회
     */
    public ConnectionStatistics getConnectionStatistics() {
        return connectionManager.getStatistics();
    }

    /**
     * 전송 통계 조회
     */
    public CommunicationStats getCommunicationStats() {
        return CommunicationStats.builder()
                .totalMessagesSent(totalMessagesSent.get())
                .totalBroadcastsSent(totalBroadcastsSent.get())
                .totalGroupMessagesSent(totalGroupMessagesSent.get())
                .activeConnections(connectionManager.getActiveConnectionCount())
                .requestListSize(requestClientList.size())
                .build();
    }

    /**
     * 전송 이력 조회
     */
    public Map<String, Byte> getTransmissionHistory() {
        return trackingService.getClientTransmissionHistory();
    }

    /**
     * 특정 클라이언트의 마지막 전송 opcode 조회
     */
    public Byte getLastTransmittedOpcode(String clientId) {
        return trackingService.getLastTransmittedOpcode(clientId);
    }

    /**
     * 지원하는 opcode 목록 반환
     */
    public Set<Byte> getSupportedOpcodes() {
        return messageBuilder.getSupportedOpcodes();
    }

    // =================== 검증 메서드들 ===================

    private void validateClientForSend(String clientId) {
        if (!MessageUtils.isValidClientId(clientId)) {
            throw new IllegalArgumentException("Invalid client ID: " + clientId);
        }
        if (!connectionManager.isActive(clientId)) {
            throw new IllegalStateException("Client is not active: " + clientId);
        }
    }

    private void validateOpcode(byte opcode) {
        if (!messageBuilder.isOpcodeSupported(opcode)) {
            throw new IllegalArgumentException("Unsupported opcode: " +
                    MessageUtils.formatOpcode(opcode));
        }
    }

    private void validateMessageData(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Message data cannot be null or empty");
        }
    }

    // =================== 정리 작업 ===================

    @PreDestroy
    public void cleanup() {
        logger.info("CommunicationService cleanup started");
        requestClientList.clear();
        logger.info("CommunicationService cleanup completed - Stats: Messages={}, Broadcasts={}, Groups={}",
                totalMessagesSent.get(), totalBroadcastsSent.get(), totalGroupMessagesSent.get());
    }

    // =================== 레거시 호환성 메서드들 ===================

    @Deprecated(since = "2.0", forRemoval = true)
    public void addClient(String clientId) {
        addToRequestList(clientId);
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public void deleteClient(String clientId) {
        removeClient(clientId);
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public Set<String> getActiveClientIds() {
        return getActiveClients();
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public Set<String> getReqClientIdList() {
        return getRequestClientIds();
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public void sendToSingleClient(String clientId, byte opcode) {
        sendMessage(clientId, opcode);
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public void sendToSingleClient(String clientId, byte[] data) {
        sendMessage(clientId, data);
    }

    @Deprecated(since = "2.0", forRemoval = true)
    public Map<String, Byte> getDataSendHistory() {
        return getTransmissionHistory();
    }
}