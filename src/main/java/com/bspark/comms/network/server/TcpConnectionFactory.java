package com.bspark.comms.network.server;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TcpConnectionFactory {

    private static final Logger logger = LoggerFactory.getLogger(TcpConnectionFactory.class);
    
    private final AcceptClientLoop acceptClientLoop;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * TCP 서버 시작
     */
    public void startTcpServer(int port, Set<String> whiteList) {
        try {
            logger.info("Starting TCP server on port {} with whitelist: {}", port, whiteList);
            
            // AcceptClientLoop를 통해 서버 시작
            acceptClientLoop.startServer(port);
            
            logger.info("TCP server started successfully on port {}", port);
            
        } catch (Exception e) {
            logger.error("Failed to start TCP server on port {}: {}", port, e.getMessage(), e);
            throw new RuntimeException("TCP server startup failed", e);
        }
    }

    /**
     * TCP 서버 중지
     */
    public void stopTcpServer() {
        try {
            logger.info("Stopping TCP server...");
            acceptClientLoop.stopServer();
            logger.info("TCP server stopped successfully");
        } catch (Exception e) {
            logger.error("Error stopping TCP server: {}", e.getMessage(), e);
        }
    }

    /**
     * 서버 소켓 생성
     */
    private ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }
}