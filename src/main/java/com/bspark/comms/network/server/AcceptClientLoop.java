
package com.bspark.comms.network.server;

import com.bspark.comms.core.connection.ConnectionManager;
import com.bspark.comms.network.handler.ConnectionHandler;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class AcceptClientLoop implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AcceptClientLoop.class);

    private final ConnectionManager connectionManager;
    private final ConnectionHandler connectionHandler;

    @Qualifier("tcpServerExecutor")
    private final ExecutorService tcpServerExecutor;

    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger connectionSequence = new AtomicInteger(0);

    public void startServer(int port) {
        if (running.get()) {
            logger.warn("Server is already running");
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(1000); // 1초 accept 타임아웃

            running.set(true);
            tcpServerExecutor.submit(this);

            logger.info("TCP Server started on port {}", port);
        } catch (IOException e) {
            logger.error("Failed to start server on port {}: {}", port, e.getMessage());
            running.set(false);
        }
    }

    @Override
    public void run() {
        logger.info("Accept client loop started");

        while (running.get()) {
            try {
                if (serverSocket == null || serverSocket.isClosed()) {
                    logger.warn("Server socket is null or closed, stopping accept loop");
                    break;
                }

                Socket clientSocket = serverSocket.accept();

                if (clientSocket != null) {
                    String clientId = generateClientId(clientSocket);

                    // 중복 연결 체크
                    if (connectionManager.isActive(clientId)) {
                        logger.warn("Duplicate connection attempt from {}, closing new connection", clientId);
                        closeSocketSafely(clientSocket);
                        continue;
                    }

                    // 새로운 연결 처리
                    connectionHandler.handleNewConnection(clientId, clientSocket);
                    logger.info("New client connected: {} [Total: {}]",
                            clientId, connectionManager.getActiveConnectionCount());
                }

            } catch (java.net.SocketTimeoutException e) {
                // 타임아웃은 정상적인 상황 (1초마다 running 상태 체크)
                continue;

            } catch (SocketException e) {
                if (running.get()) {
                    logger.error("Socket error in accept loop: {}", e.getMessage());
                } else {
                    logger.debug("Accept loop stopped due to socket closure");
                }
                break;

            } catch (IOException e) {
                if (running.get()) {
                    logger.error("Error accepting client connection: {}", e.getMessage());
                } else {
                    logger.debug("Accept loop stopped");
                }
                break;

            } catch (Exception e) {
                logger.error("Unexpected error in accept loop: {}", e.getMessage(), e);
                break;
            }
        }

        logger.info("Accept client loop ended");
    }

    public void stopServer() {
        if (!running.get()) {
            return;
        }

        logger.info("Stopping TCP server...");
        running.set(false);

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("TCP Server stopped successfully");
            } catch (IOException e) {
                logger.error("Error stopping server: {}", e.getMessage());
            }
        }
    }

    /**
     * 클라이언트 ID 생성 (안정적인 ID 생성)
     */
    private String generateClientId(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        int clientPort = socket.getPort();
        int sequence = connectionSequence.incrementAndGet();

        // IP:Port-Sequence 형태로 고유한 ID 생성
        //return String.format("%s:%d-%d", clientIp, clientPort, sequence);
        return clientIp;
    }

    /**
     * 소켓 안전 종료
     */
    private void closeSocketSafely(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.warn("Error closing socket: {}", e.getMessage());
            }
        }
    }

    /**
     * 서버 상태 확인
     */
    public boolean isRunning() {
        return running.get() && serverSocket != null && !serverSocket.isClosed();
    }

    /**
     * 서버 포트 반환
     */
    public int getServerPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : -1;
    }

    @PreDestroy
    public void cleanup() {
        stopServer();
    }
}