package com.bspark.comms.message;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageSendHistoryService {

    @Getter
    private final Map<String, Byte> clientTransmissionHistory = new ConcurrentHashMap<>();

    public void recordTransmission(String clientId, byte opcode) {
        clientTransmissionHistory.put(clientId, opcode);
    }

    public Byte getLastTransmittedOpcode(String clientId) {
        return clientTransmissionHistory.get(clientId);
    }

    public void clearHistory(String clientId) {
        clientTransmissionHistory.remove(clientId);
    }

    public void clearAllHistory() {
        clientTransmissionHistory.clear();
    }
}