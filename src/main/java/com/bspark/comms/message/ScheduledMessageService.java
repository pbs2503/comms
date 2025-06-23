package com.bspark.comms.message;

import com.bspark.comms.core.connection.ConnectionManager;
import com.bspark.comms.core.protocol.message.MessageBuilder;
import com.bspark.comms.network.transport.MessageSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ScheduledMessageService {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledMessageService.class);

    private final MessageSender messageSender;
    private final MessageBuilder messageBuilder;
    private final ConnectionManager connectionManager;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "scheduled-message-service");
                t.setDaemon(true);
                return t;
            });

    @PostConstruct
    public void startStatusRequestScheduler() {
        scheduler.scheduleAtFixedRate(this::sendStatusRequestToAllClients, 0, 1, TimeUnit.SECONDS);
        logger.info("Status request scheduler started");
    }

    private void sendStatusRequestToAllClients() {
        if (connectionManager.getActiveConnectionCount() == 0) {
            return;
        }

        try {
            byte[] statusRequestMessage = messageBuilder.buildMessage((byte) 0x12);
            messageSender.sendMessageToAllActiveClients(statusRequestMessage);
            logger.debug("Status request sent to {} active clients",
                    connectionManager.getActiveConnectionCount());
        } catch (Exception e) {
            logger.error("Error sending status request to clients: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ScheduledMessageService...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("ScheduledMessageService shutdown completed");
    }
}