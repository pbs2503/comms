package com.bspark.comms.network.server;

import com.bspark.comms.network.server.nio.NioConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TcpClientService {
    private static final Logger logger = LoggerFactory.getLogger(TcpClientService.class);

    private final NioConnectionManager connectionManager;

    public TcpClientService(NioConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * 특정 클라이언트에게 데이터 전송
     */
    public boolean sendDataToClient(String clientId, byte[] data) {
        logger.debug("클라이언트에게 데이터 전송: {} ({} 바이트)", clientId, data.length);
        return connectionManager.sendData(clientId, data);
    }

    public int sendDataToAllActiveClients(byte[] data) {
        Map<String, String> connectedClients = connectionManager.getConnectedClients();

        if (connectedClients.isEmpty()) {
            logger.debug("브로드캐스트할 활성 클라이언트가 없습니다");
            return 0;
        }

        int successCount = 0;
        int totalClients = connectedClients.size();

        logger.info("모든 클라이언트에게 브로드캐스트 시작: {} 클라이언트 ({} 바이트)",
                totalClients, data.length);

        for (String clientId : connectedClients.keySet()) {
            try {
                if (connectionManager.sendData(clientId, data)) {
                    successCount++;
                    logger.debug("브로드캐스트 성공: {}", clientId);
                } else {
                    logger.warn("브로드캐스트 실패: {}", clientId);
                }
            } catch (Exception e) {
                logger.error("브로드캐스트 중 오류 발생 ({}): {}", clientId, e.getMessage());
            }
        }

        logger.info("브로드캐스트 완료: {}/{} 클라이언트 성공", successCount, totalClients);
        return successCount;
    }


    /**
     * 클라이언트 연결 종료
     */
    public boolean disconnectClient(String clientId) {
        logger.info("클라이언트 연결 종료 요청: {}", clientId);
        return connectionManager.disconnectClient(clientId);
    }

    /**
     * 연결된 클라이언트 수 반환
     */
    public int getConnectionCount() {
        return connectionManager.getActiveConnectionCount();
    }

    /**
     * 연결된 클라이언트 목록 반환
     */
    public Map<String, String> getConnectedClients() {
        return connectionManager.getConnectedClients();
    }
}