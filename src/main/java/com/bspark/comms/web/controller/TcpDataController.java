
package com.bspark.comms.web.controller;

import com.bspark.comms.service.communication.CommunicationService;
import com.bspark.comms.network.transport.MessageSendException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TcpDataController {

    private static final Logger logger = LoggerFactory.getLogger(TcpDataController.class);
    private final CommunicationService communicationService;

    @PostMapping("/broadcasts/0x12")
    public ResponseEntity<Boolean> receiveTransmissionRequest(@RequestBody byte[] body) {
        try {
            communicationService.broadcastMessage((byte) 0x12);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            logger.error("Error processing broadcast request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(false);
        }
    }

    @PostMapping("/unicast/{clientId:.+}/{opcode}")
    public ResponseEntity<Boolean> sendCommandToTSC(
            @PathVariable String clientId,
            @PathVariable String opcode) {
        try {
            byte op = parseOpcode(opcode);
            communicationService.sendMessage(clientId, op);
            return ResponseEntity.ok(true);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(false);
        } catch (MessageSendException e) {
            return ResponseEntity.internalServerError().body(false);
        }
    }

    private byte parseOpcode(String opcode) {
        try {
            if (opcode.startsWith("0x") || opcode.startsWith("0X")) {
                return (byte) Integer.parseInt(opcode.substring(2), 16);
            } else {
                return (byte) Integer.parseInt(opcode, 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid opcode format: " + opcode);
        }
    }
}