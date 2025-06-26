
package com.bspark.comms.network.server;

import com.bspark.comms.data.MessageType;
import com.bspark.comms.events.DataReceivedEvent;
import com.bspark.comms.message.MessageProcessor;
import com.bspark.comms.service.external.HttpClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class TcpDataHandler {
    private static final Logger logger = LoggerFactory.getLogger(TcpDataHandler.class);

    private final MessageProcessor messageProcessor;
    private final TcpClientService tcpClientService;
    private final HttpClientService httpClientService; // 추가

    private final ExecutorService processingExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "tcp-data-handler");
                t.setDaemon(true);
                return t;
            }
    );

    /**
     * 데이터 수신 이벤트 처리
     */
    @EventListener
    public void handleDataReceived(DataReceivedEvent event) {
        String clientId = event.getClientId();
        byte[] data = event.getData();

        logger.debug("데이터 수신 처리: {} ({} 바이트)", clientId, data.length);

        // 데이터 처리를 별도 스레드 풀에서 비동기 처리
        processingExecutor.submit(() -> processReceivedData(clientId, event.getMessageType(), data));
    }

    /**
     * 수신된 데이터 처리
     */
    private void processReceivedData(String clientId, MessageType messageType, byte[] data) {
        try {
            // 1. 기존 HTTP API로 데이터 전송 (Redis/외부 시스템으로)
            logger.debug("외부 API로 데이터 전송: 클라이언트={}, 유형={}", clientId, messageType);
            httpClientService.sendDataAsync(clientId, messageType, data);

            // 2. 메시지 처리기를 통해 데이터 처리 및 응답 생성
            byte[] response = messageProcessor.processMessage(clientId, messageType, data);

            // 3. 응답이 있다면 클라이언트에게 전송
            if (response != null && response.length > 0) {
                tcpClientService.sendDataToClient(clientId, response);
                logger.debug("응답 전송 완료: {} ({} 바이트)", clientId, response.length);
            }

        } catch (Exception e) {
            logger.error("데이터 처리 중 오류 발생 (클라이언트: {}): {}", clientId, e.getMessage(), e);
        }
    }
}