
package com.bspark.comms.network.handler;

import com.bspark.comms.core.connection.ConnectionManager;
import com.bspark.comms.core.protocol.message.MessageProcessor;
import com.bspark.comms.event.connection.ConnectionEstablishedEvent;
import com.bspark.comms.event.connection.ConnectionTerminatedEvent;
import com.bspark.comms.event.message.MessageReceivedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
public class ConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

    private final ConnectionManager connectionManager;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageProcessor messageProcessor;

    @Qualifier("tcpServerExecutor")
    private final ExecutorService tcpServerExecutor;

    /**
     * 새로운 클라이언트 연결 처리
     */
    public void handleNewConnection(String clientId, Socket clientSocket) {
        try {
            // 소켓 설정
            configureSocket(clientSocket);

            // 연결 등록
            connectionManager.addConnection(clientId, clientSocket);

            // 연결 성공 이벤트 발행
            publishConnectionEstablishedEvent(clientId, clientSocket);

            // 비동기 데이터 처리 시작
            startAsyncDataProcessing(clientId, clientSocket);

        } catch (IOException e) {
            logger.error("Failed to handle new connection for client {}: {}", clientId, e.getMessage());
            closeSocketSafely(clientId, clientSocket);
        }
    }

    /**
     * 소켓 설정
     */
    private void configureSocket(Socket socket) throws IOException {
        socket.setKeepAlive(true);
        socket.setSoTimeout(30000); // 30초 읽기 타임아웃
        socket.setTcpNoDelay(true);
        socket.setReceiveBufferSize(8192);
        socket.setSendBufferSize(8192);

        logger.debug("Socket configured: keepAlive=true, timeout=30s, tcpNoDelay=true");
    }

    /**
     * 연결 성공 이벤트 발행
     */
    private void publishConnectionEstablishedEvent(String clientId, Socket socket) {
        int clientPort = socket.getPort();

        eventPublisher.publishEvent(new ConnectionEstablishedEvent(this, clientId, socket));
        logger.info("Connection established: {} from {}:{}", clientId, clientId, clientPort);
    }

    /**
     * 비동기 데이터 처리 시작
     */
    private void startAsyncDataProcessing(String clientId, Socket socket) {
        CompletableFuture.runAsync(() -> processClientData(clientId, socket), tcpServerExecutor)
                .exceptionally(throwable -> {
                    logger.error("Async data processing failed for client {}: {}", clientId, throwable.getMessage());
                    handleConnectionTermination(clientId, socket.getInetAddress().getHostAddress(),
                            "Async processing error: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * 클라이언트 데이터 처리 (바이너리 데이터)
     */
    private void processClientData(String clientId, Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        logger.debug("Started data processing for client: {}", clientId);

        try (InputStream inputStream = socket.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while (!socket.isClosed() && socket.isConnected()) {
                try {
                    bytesRead = inputStream.read(buffer);

                    if (bytesRead == -1) {
                        logger.debug("Client {} disconnected (EOF)", clientId);
                        break;
                    }

                    if (bytesRead > 0) {
                        // 활동 시간 업데이트
                        connectionManager.updateActivity(clientId);

                        // 수신된 데이터 처리
                        byte[] receivedData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, receivedData, 0, bytesRead);

                        // 메시지 수신 이벤트 발행
                        eventPublisher.publishEvent(new MessageReceivedEvent(clientId, receivedData));

                        logger.debug("Received {} bytes from client {}", bytesRead, clientId);
                    }

                } catch (SocketTimeoutException e) {
                    // 타임아웃은 정상적인 상황 (30초마다 발생)
                    logger.trace("Socket timeout for client {} - checking connection", clientId);

                    // 연결 상태 확인
                    if (!connectionManager.isActive(clientId)) {
                        logger.debug("Client {} is no longer active, terminating connection", clientId);
                        break;
                    }

                } catch (SocketException e) {
                    if ("Connection reset".equals(e.getMessage()) || "Connection reset by peer".equals(e.getMessage())) {
                        logger.debug("Client {} reset connection", clientId);
                    } else {
                        logger.warn("Socket error for client {}: {}", clientId, e.getMessage());
                    }
                    break;
                }
            }

        } catch (IOException e) {
            logger.warn("Connection lost for client {}: {}", clientId, e.getMessage());
        } finally {
            handleConnectionTermination(clientId, clientIp, "Data processing completed");
        }
    }

    /**
     * 연결 종료 처리
     */
    private void handleConnectionTermination(String clientId, String clientIp, String reason) {
        // 연결 매니저에서 제거
        connectionManager.removeConnection(clientId);

        // 연결 종료 이벤트 발행
        eventPublisher.publishEvent(new ConnectionTerminatedEvent(clientId, clientIp, reason));

        logger.info("Connection terminated: {} from {} - {}", clientId, clientIp, reason);
    }

    /**
     * 소켓 안전 종료
     */
    private void closeSocketSafely(String clientId, Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                // Graceful shutdown
                if (!socket.isOutputShutdown()) {
                    socket.shutdownOutput();
                }
                if (!socket.isInputShutdown()) {
                    socket.shutdownInput();
                }
                socket.close();
                logger.debug("Socket closed safely for client: {}", clientId);
            } catch (IOException e) {
                logger.warn("Error closing socket for client {}: {}", clientId, e.getMessage());
            }
        }
    }

    /**
     * 특정 클라이언트 연결 강제 종료
     */
    public void forceDisconnect(String clientId, String reason) {
        Socket socket = connectionManager.getSocket(clientId);
        if (socket != null) {
            logger.info("Force disconnecting client: {} - {}", clientId, reason);
            closeSocketSafely(clientId, socket);
            handleConnectionTermination(clientId, socket.getInetAddress().getHostAddress(),
                    "Force disconnect: " + reason);
        }
    }

    /**
     * 하트비트 체크 (외부에서 호출)
     */
    public void checkHeartbeat(String clientId) {
        if (!connectionManager.isActive(clientId)) {
            logger.debug("Heartbeat check failed for inactive client: {}", clientId);
            return;
        }

        Socket socket = connectionManager.getSocket(clientId);
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            logger.debug("Heartbeat check detected dead connection for client: {}", clientId);
            handleConnectionTermination(clientId,
                    socket != null ? socket.getInetAddress().getHostAddress() : "unknown",
                    "Heartbeat check failed");
        }
    }
}