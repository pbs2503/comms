
package com.bspark.comms.web.controller;

import com.bspark.comms.core.protocol.message.MessageBuilder;
import com.bspark.comms.network.server.TcpClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transmit")
@RequiredArgsConstructor
public class HttpRequestTransmitter {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestTransmitter.class);

    @Autowired
    private final TcpClientService tcpClientService;

    @Autowired
    private final MessageBuilder messageBuilder;


    @PostMapping("/broadcast/{opcode}")
    public ResponseEntity<String> broadcastMessage(@PathVariable String opcode) {
        try {
            byte op = parseOpcode(opcode);
            byte[] data = messageBuilder.buildMessage(op);
            tcpClientService.sendDataToAllActiveClients(data);
            return ResponseEntity.ok("Broadcast sent successfully");
        } catch (Exception e) {
            logger.error("Broadcast failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Broadcast failed: " + e.getMessage());
        }
    }

    @PostMapping("/unicast/{clientId}/{opcode}")
    public ResponseEntity<String> sendUnicast(
            @PathVariable String clientId,
            @PathVariable String opcode) {
        try {
            byte op = parseOpcode(opcode);
            byte[] data = messageBuilder.buildMessage(op);

            tcpClientService.sendDataToClient(clientId, data);

            return ResponseEntity.ok("Message sent to " + clientId);
        } catch (Exception e) {
            logger.error("Unicast failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Unicast failed: " + e.getMessage());
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