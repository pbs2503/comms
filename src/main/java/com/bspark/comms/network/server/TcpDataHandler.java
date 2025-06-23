package com.bspark.comms.network.server;

import com.bspark.comms.core.connection.ConnectionManager;
import com.bspark.comms.event.connection.ConnectionEstablishedEvent;
import com.bspark.comms.event.connection.ConnectionTerminatedEvent;
import com.bspark.comms.event.message.MessageReceivedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class TcpDataHandler {

    private static final Logger logger = LoggerFactory.getLogger(TcpDataHandler.class);

    private final ApplicationEventPublisher eventPublisher;
    private final ConnectionManager connectionManager;

    private final ExecutorService clientHandlerExecutor = Executors.newFixedThreadPool(20,
            r -> {
                Thread t = new Thread(r, "tcp-client-handler");
                t.setDaemon(true);
                return t;
            });

    /**
     * 새 클라이언트 연결 처리
     */
    @EventListener
    public void handleConnectionEstablished(ConnectionEstablishedEvent event) {
        String clientId = event.getClientId();
        Socket socket = event.getSocket();

        logger.info("Starting data handler for client: {}", clientId);
        clientHandlerExecutor.submit(() -> handleClientData(clientId, socket));
    }

    /**
     * 클라이언트 데이터 처리
     */
    private void handleClientData(String clientId, Socket socket) {
        logger.debug("Data handler started for client: {}", clientId);

        try (InputStream inputStream = socket.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while (!socket.isClosed() && (bytesRead = inputStream.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    byte[] receivedData = Arrays.copyOfRange(buffer, 0, bytesRead);

                    // 연결 활동 업데이트
                    connectionManager.updateActivity(clientId);

                    // 메시지 수신 이벤트 발행
                    MessageReceivedEvent messageEvent = new MessageReceivedEvent(
                            clientId, receivedData, System.currentTimeMillis());
                    eventPublisher.publishEvent(messageEvent);

                    logger.debug("Data received from client {}: {} bytes", clientId, bytesRead);
                }
            }

        } catch (IOException e) {
            if (!socket.isClosed()) {
                logger.warn("Error reading data from client {}: {}", clientId, e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Unexpected error handling client {}: {}", clientId, e.getMessage(), e);
        } finally {
            // 연결 종료 이벤트 발행
            eventPublisher.publishEvent(new ConnectionTerminatedEvent(
                    clientId, socket.getInetAddress().getHostAddress(), "Data handler completed"));
            logger.debug("Data handler completed for client: {}", clientId);
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down TcpDataHandler...");
        clientHandlerExecutor.shutdown();
        try {
            if (!clientHandlerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                clientHandlerExecutor.shutdownNow();
                if (!clientHandlerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("TcpDataHandler did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            clientHandlerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("TcpDataHandler shutdown completed");
    }
}