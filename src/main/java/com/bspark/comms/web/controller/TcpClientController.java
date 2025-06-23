package com.bspark.comms.web.controller;

import com.bspark.comms.service.communication.CommunicationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TcpClientController {

    private static final Logger logger = LoggerFactory.getLogger(TcpClientController.class);
    private final CommunicationService communicationService;

    @PostMapping("/clients/{clientId:.+}")
    public ResponseEntity<Void> addClientIdToRequestStatusInfoList(@PathVariable String clientId) {
        try {
            if (communicationService.isClientActive(clientId)) {
                logger.info("Client {} is already active", clientId);
            } else {
                logger.warn("Attempting to add inactive client {} to request list", clientId);
            }

            communicationService.addToRequestList(clientId);
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (Exception e) {
            logger.error("Error adding client {} to request list: {}", clientId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/clients")
    public ResponseEntity<Set<String>> getClientIdList() {
        try {
            Set<String> activeClients = communicationService.getActiveClients();
            logger.debug("Retrieved {} active clients", activeClients.size());
            return ResponseEntity.ok(activeClients);

        } catch (Exception e) {
            logger.error("Error retrieving client list: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/clients/{clientId:.+}")
    public ResponseEntity<Void> removeClientIdFromRequestStatusInfoList(@PathVariable String clientId) {
        try {
            communicationService.removeFromRequestList(clientId);
            logger.info("Client {} removed from request list", clientId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

        } catch (Exception e) {
            logger.error("Error removing client {} from request list: {}", clientId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/clients/{clientId:.+}/status")
    public ResponseEntity<Object> getClientStatus(@PathVariable String clientId) {
        try {
            boolean isActive = communicationService.isClientActive(clientId);

            return ResponseEntity.ok(java.util.Map.of(
                    "clientId", clientId,
                    "active", isActive,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            logger.error("Error checking status for client {}: {}", clientId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/clients/inactive")
    public ResponseEntity<Object> cleanupInactiveClients() {
        try {
            int removedCount = communicationService.cleanupInactiveClients();

            return ResponseEntity.ok(java.util.Map.of(
                    "removedCount", removedCount,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            logger.error("Error cleaning up inactive clients: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}