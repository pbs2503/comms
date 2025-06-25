package com.bspark.comms.network.handler;

import com.bspark.comms.core.protocol.message.MessageProcessor;
import com.bspark.comms.data.Message;
import com.bspark.comms.event.message.MessageReceivedEvent;
import com.bspark.comms.service.external.HttpClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final MessageProcessor messageProcessor;
    private final HttpClientService httpClientService;

    @Async
    @EventListener
    public void handleMessageReceived(MessageReceivedEvent event) {
        // 올바른 Message 타입 사용
        Message message = messageProcessor.processRawMessage(event.getClientId(), event.getData());

        if (message == null) {
            logger.warn("Failed to process message from client: {}", event.getClientId());
            return;
        }

        logger.debug("Processing message: {}", message);
        processMessage(message);
    }

    private void processMessage(Message message) {
        String clientId = message.getClientId();
        byte opcode = message.getOpcode();
        byte[] data = message.getData();

        logger.debug("Processing intersection status data from {}", clientId);
        httpClientService.sendDataAsync(clientId, message.getType(), data);

/*
        switch (message.getType()) {
            case INTERSECTION_STATUS -> {
                logger.debug("Processing intersection status data from {}", clientId);
                httpClientService.sendDataAsync(clientId, message.getType(), data);
            }
            case DETECTOR_INFO, PHASE_INFO ->
                    logger.debug("Processing detector/phase data from {} [opcode: {}]",
                            clientId, message.getOpcodeAsHex());
            default ->
                    handleUserRequestData(message);
        }
*/
    }

    private void handleUserRequestData(Message message) {
        logger.debug("Handling user request data from {}: {} bytes",
                message.getClientId(), message.getSize());
        
        // 사용자 요청 데이터 처리 로직
        // 필요에 따라 외부 API로 전송하거나 내부 처리
        if (message.getSize() > 0) {
            httpClientService.sendDataAsync(message.getClientId(), message.getType(), message.getData());
        }
    }
}