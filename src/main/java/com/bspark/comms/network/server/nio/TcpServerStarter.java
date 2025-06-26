
package com.bspark.comms.network.server.nio;

import com.bspark.comms.config.CommsProperties;
import com.bspark.comms.dao.IpAddressWhitelistDAO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class TcpServerStarter {

    private static final Logger logger = LoggerFactory.getLogger(TcpServerStarter.class);

    private final NioTcpServer nioTcpServer;
    private final CommsProperties commsProperties;
    private final IpAddressWhitelistDAO ipAddressWhitelistDAO; // PostgreSQL DAO 사용

    @PostConstruct
    public void startTcpServer() {
        if (commsProperties.getServer().isAutoStart()) {
            logger.info("TCP 서버 자동 시작 중...");

            // PostgreSQL에서 화이트리스트 조회
            Set<String> whiteList = getWhiteListFromDatabase();

            nioTcpServer.start(commsProperties.getServer().getPort(), whiteList);
        }
    }

    /**
     * PostgreSQL에서 화이트리스트 IP 목록 조회
     */
    private Set<String> getWhiteListFromDatabase() {
        try {
            // PostgreSQL에서 활성 IP 목록 가져오기
            Set<String> dbWhiteList = ipAddressWhitelistDAO.getAllowedIps();

            logger.info("PostgreSQL에서 {} 개의 화이트리스트 IP 로드됨", dbWhiteList.size());

            // 기본 localhost는 항상 포함
            dbWhiteList.add("127.0.0.1");

            return dbWhiteList;

        } catch (Exception e) {
            logger.error("PostgreSQL에서 화이트리스트 로드 실패, 기본값 사용: {}", e.getMessage());

            // DB 연결 실패 시 기본 화이트리스트 반환
            return Set.of("127.0.0.1", "192.168.1.100");
        }
    }

    @PreDestroy
    public void stopTcpServer() {
        logger.info("TCP 서버 종료 중...");
        nioTcpServer.stop();
    }
}