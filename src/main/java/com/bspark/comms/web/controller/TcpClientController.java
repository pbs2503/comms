package com.bspark.comms.web.controller;

import com.bspark.comms.network.server.TcpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import java.util.Map;

@RestController
@RequestMapping("/api/tcp/clients")
@RequiredArgsConstructor
public class TcpClientController {
    private static final Logger logger = LoggerFactory.getLogger(TcpClientController.class);

    private final TcpClientService tcpClientService;

    /**
     * 연결된 모든 클라이언트 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getConnectedClients() {
        Map<String, String> clients = tcpClientService.getConnectedClients();
        return ResponseEntity.ok(clients);
    }

    /**
     * 연결된 클라이언트 수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> getConnectionCount() {
        int count = tcpClientService.getConnectionCount();
        return ResponseEntity.ok(count);
    }

    /**
     * 특정 클라이언트 연결 종료
     */
    @DeleteMapping("/{clientId}")
    public ResponseEntity<String> disconnectClient(@PathVariable String clientId) {
        logger.info("클라이언트 연결 종료 요청: {}", clientId);

        boolean success = tcpClientService.disconnectClient(clientId);
        if (success) {
            return ResponseEntity.ok("Client disconnected successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Client not found or already disconnected");
        }
    }
}