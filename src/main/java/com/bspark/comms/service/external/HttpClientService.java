package com.bspark.comms.service.external;

import com.bspark.comms.data.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class HttpClientService {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientService.class);

    @Value("${comms.external.api.base-url:http://localhost:8115}")
    private String baseUrl;

    @Value("${comms.external.api.timeout:5000}")
    private int timeoutMillis;

    @Value("${comms.external.api.enabled:true}")
    private boolean apiEnabled;

    private final RestTemplate restTemplate;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10,
            r -> {
                Thread t = new Thread(r, "http-client");
                t.setDaemon(true);
                return t;
            });

    /**
     * 데이터 전송 (동기)
     */
    public void sendData(String clientId, MessageType msgType, byte[] data) {
        if (!apiEnabled) {
            logger.debug("External API is disabled, skipping data transmission");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-TSC-IP", clientId);

            DataPayload payload = DataPayload.builder()
                    .clientId(clientId)
                    .type(msgType)
                    .data(bytesToHex(data))
                    .timestamp(System.currentTimeMillis())
                    .dataLength(data.length)
                    .build();

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            System.out.println("Outgoing Payload:\n" + json);

            HttpEntity<DataPayload> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/resp-0x12", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Data sent successfully for client {}: {} bytes", clientId, data.length);
            } else {
                logger.warn("Unexpected response for client {}: {}", clientId, response.getStatusCode());
            }

        } catch (RestClientException e) {
            logger.error("Failed to send data for client {}: {}", clientId, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending data for client {}: {}", clientId, e.getMessage(), e);
        }
    }

    /**
     * 데이터 전송 (비동기)
     */
    public CompletableFuture<Void> sendDataAsync(String clientId, MessageType msgType, byte[] data) {
        return CompletableFuture.runAsync(() -> sendData(clientId, msgType, data), executorService);
    }

    /**
     * 연결 테스트
     */
    public boolean testConnection() {
        if (!apiEnabled) {
            return false;
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 서비스 상태 확인
     */
    public ServiceStatus getServiceStatus() {
        return ServiceStatus.builder()
                .enabled(apiEnabled)
                .baseUrl(baseUrl)
                .timeout(timeoutMillis)
                .connected(testConnection())
                .build();
    }

    /**
     * byte 배열을 16진수 문자열로 변환
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down HttpClientService...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("HttpClientService shutdown completed");
    }

    /**
     * 데이터 페이로드
     */
    @lombok.Getter
    @lombok.Builder
    public static class DataPayload {
        private final String clientId;
        private final MessageType type;
        private final String data;
        private final long timestamp;
        private final int dataLength;
    }

    /**
     * 서비스 상태
     */
    @lombok.Getter
    @lombok.Builder
    public static class ServiceStatus {
        private final boolean enabled;
        private final String baseUrl;
        private final int timeout;
        private final boolean connected;

        @Override
        public String toString() {
            return String.format("ServiceStatus{enabled=%s, connected=%s, url='%s'}",
                    enabled, connected, baseUrl);
        }
    }
}