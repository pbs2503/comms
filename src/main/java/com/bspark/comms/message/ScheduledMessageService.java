package com.bspark.comms.message;

import com.bspark.comms.core.protocol.message.MessageBuilder;
import com.bspark.comms.events.ClientConnectedEvent;
import com.bspark.comms.events.ClientDisconnectedEvent;
import com.bspark.comms.network.server.TcpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ScheduledMessageService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledMessageService.class);

    private final TcpClientService tcpClientService;
    private final MessageFactory messageFactory;
    private final Map<String, ScheduledExecutorService> clientSchedulers = new ConcurrentHashMap<>();

    @Autowired
    private final MessageBuilder messageBuilder;

    public ScheduledMessageService(TcpClientService tcpClientService, MessageFactory messageFactory, MessageBuilder messageBuilder) {
        this.tcpClientService = tcpClientService;
        this.messageFactory = messageFactory;
        this.messageBuilder = messageBuilder;
    }

    /**
     * 클라이언트 연결 시 정기 메시지 전송 스케줄러 시작
     */
    @EventListener
    public void handleClientConnected(ClientConnectedEvent event) {
        String clientId = event.getClientId();
        logger.info("클라이언트 연결됨: {} - 정기 메시지 전송 시작", clientId);

        // 클라이언트별 스케줄러 생성
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "scheduled-sender-" + clientId);
            t.setDaemon(true);
            return t;
        });

        // 1초 간격으로 메시지 전송 스케줄링
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendPeriodicMessageToClient(clientId);
            } catch (Exception e) {
                logger.error("정기 메시지 전송 중 오류 발생 (클라이언트: {}): {}", clientId, e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);

        clientSchedulers.put(clientId, scheduler);
    }

    /**
     * 클라이언트 연결 종료 시 스케줄러 정리
     */
    @EventListener
    public void handleClientDisconnected(ClientDisconnectedEvent event) {
        String clientId = event.getClientId();
        logger.info("클라이언트 연결 종료됨: {} - 정기 메시지 전송 중단", clientId);

        ScheduledExecutorService scheduler = clientSchedulers.remove(clientId);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * 주기적으로 연결된 모든 클라이언트에 상태 확인 메시지 전송
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void sendHeartbeatToAllClients() {
        int clientCount = tcpClientService.getConnectionCount();
        if (clientCount > 0) {
            logger.debug("하트비트 메시지 전송 (연결된 클라이언트: {}개)", clientCount);

            tcpClientService.getConnectedClients().keySet().forEach(clientId -> {
                try {
                    byte[] heartbeatData = messageFactory.createHeartbeatMessage();
                    tcpClientService.sendDataToClient(clientId, heartbeatData);
                } catch (Exception e) {
                    logger.warn("하트비트 메시지 전송 실패 (클라이언트: {}): {}", clientId, e.getMessage());
                }
            });
        }
    }

    /**
     * 특정 클라이언트에 주기적으로 메시지 전송
     */
    private void sendPeriodicMessageToClient(String clientId) {
        // 현재 시간을 포함한 상태 요청 메시지 생성
        //byte[] statusRequestData = messageFactory.createStatusRequestMessage();

        byte[] statusRequestData = messageBuilder.buildMessage((byte)0x12);

        boolean sent = tcpClientService.sendDataToClient(clientId, statusRequestData);
        if (sent) {
            logger.debug("정기 상태 요청 메시지 전송됨: {}", clientId);
        } else {
            logger.warn("정기 상태 요청 메시지 전송 실패: {}", clientId);

            // 전송 실패 시 스케줄러 정리 (클라이언트 연결이 끊어졌을 가능성)
            ScheduledExecutorService scheduler = clientSchedulers.remove(clientId);
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
        }
    }
}