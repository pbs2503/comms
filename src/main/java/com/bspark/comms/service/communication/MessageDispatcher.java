package com.bspark.comms.service.communication;

import com.bspark.comms.core.protocol.message.Message;
import com.bspark.comms.core.protocol.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class MessageDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final Map<MessageType, Consumer<Message>> messageHandlers = new ConcurrentHashMap<>();

    public MessageDispatcher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        initializeDefaultHandlers();
    }

    /**
     * 메시지 디스패치
     */
    public void dispatch(Message message) {
        if (message == null) {
            logger.warn("Attempted to dispatch null message");
            return;
        }

        MessageType messageType = message.getType();
        Consumer<Message> handler = messageHandlers.get(messageType);

        if (handler != null) {
            try {
                handler.accept(message);
                logger.debug("Message dispatched: {} from {}", messageType, message.getClientId());
            } catch (Exception e) {
                logger.error("Error dispatching message type {}: {}", messageType, e.getMessage(), e);
            }
        } else {
            logger.warn("No handler found for message type: {}", messageType);
            handleUnknownMessage(message);
        }
    }

    /**
     * 메시지 핸들러 등록
     */
    public void registerHandler(MessageType messageType, Consumer<Message> handler) {
        messageHandlers.put(messageType, handler);
        logger.info("Handler registered for message type: {}", messageType);
    }

    /**
     * 메시지 핸들러 제거
     */
    public void unregisterHandler(MessageType messageType) {
        messageHandlers.remove(messageType);
        logger.info("Handler unregistered for message type: {}", messageType);
    }

    /**
     * 기본 핸들러 초기화
     */
    private void initializeDefaultHandlers() {
        // 교차로 상태 데이터 핸들러
        registerHandler(MessageType.INTERSECTION_STATUS, this::handleIntersectionStatus);

        // 검지기 정보 핸들러
        registerHandler(MessageType.DETECTOR_INFO, this::handleDetectorInfo);

        // 현시 정보 핸들러
        registerHandler(MessageType.PHASE_INFO, this::handlePhaseInfo);

        // 사용자 요청 핸들러
        registerHandler(MessageType.USER_REQUEST, this::handleUserRequest);

        logger.info("Default message handlers initialized");
    }

    private void handleIntersectionStatus(Message message) {
        logger.debug("Handling intersection status from {}", message.getClientId());
        // 교차로 상태 처리 로직
    }

    private void handleDetectorInfo(Message message) {
        logger.debug("Handling detector info from {}", message.getClientId());
        // 검지기 정보 처리 로직
    }

    private void handlePhaseInfo(Message message) {
        logger.debug("Handling phase info from {}", message.getClientId());
        // 현시 정보 처리 로직
    }

    private void handleUserRequest(Message message) {
        logger.debug("Handling user request from {}", message.getClientId());
        // 사용자 요청 처리 로직
    }

    private void handleUnknownMessage(Message message) {
        logger.warn("Unknown message type {} from client {}: {}",
                message.getType(), message.getClientId(), message.getOpcodeAsHex());
    }

    /**
     * 등록된 핸들러 개수 반환
     */
    public int getHandlerCount() {
        return messageHandlers.size();
    }

    /**
     * 등록된 메시지 타입들 반환
     */
    public Set<MessageType> getRegisteredTypes() {
        return new HashSet<>(messageHandlers.keySet());
    }
}