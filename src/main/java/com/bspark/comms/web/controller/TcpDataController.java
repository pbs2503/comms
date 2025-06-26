package com.bspark.comms.web.controller;

import com.bspark.comms.data.MessageType;
import com.bspark.comms.network.server.TcpClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping("/api/tcp/data")
@RequiredArgsConstructor
public class TcpDataController {
    private static final Logger logger = LoggerFactory.getLogger(TcpDataController.class);

    private final TcpClientService tcpClientService;

    /**
     * HTTP API를 통해 TCP 클라이언트에게 데이터 전송
     */
    @PostMapping("/{clientId}")
    public ResponseEntity<String> sendDataToClient(
            @PathVariable String clientId,
            @RequestParam(required = false, defaultValue = "USER_REQUEST") String messageTypeName,
            @RequestBody SendDataRequest request) {

        MessageType messageType;
        try {
            messageType = MessageType.valueOf(messageTypeName);
        } catch (IllegalArgumentException e) {
            messageType = MessageType.USER_REQUEST;
        }

        logger.info("클라이언트 데이터 전송 요청: {}, 유형: {}", clientId, messageType);

        try {
            // Base64 디코딩
            byte[] binaryData = Base64.getDecoder().decode(request.getData());

            boolean success = tcpClientService.sendDataToClient(clientId, binaryData);
            if (success) {
                return ResponseEntity.ok("Data sent successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Failed to send data to client");
            }
        } catch (IllegalArgumentException e) {
            logger.error("잘못된 Base64 데이터: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid Base64 data: " + e.getMessage());
        } catch (Exception e) {
            logger.error("데이터 전송 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending data: " + e.getMessage());
        }
    }

    /**
     * 데이터 전송 요청 DTO
     */
    public static class SendDataRequest {
        private String data;  // Base64 인코딩된 데이터

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}