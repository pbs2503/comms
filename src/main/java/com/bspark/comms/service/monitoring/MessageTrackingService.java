package com.bspark.comms.service.monitoring;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MessageTrackingService {
    private static final Logger logger = LoggerFactory.getLogger(MessageTrackingService.class);

    private final Map<String, Byte> lastTransmittedOpcodes = new ConcurrentHashMap<>();
    private final Map<String, Long> transmissionTimestamps = new ConcurrentHashMap<>();

    /**
     * 전송 기록
     */
    public void recordTransmission(String clientId, byte opcode) {
        lastTransmittedOpcodes.put(clientId, opcode);
        transmissionTimestamps.put(clientId, System.currentTimeMillis());
        logger.debug("Recorded transmission for {}: opcode 0x{:02X}", clientId, opcode & 0xFF);
    }

    /**
     * 클라이언트 히스토리 삭제
     */
    public void clearHistory(String clientId) {
        lastTransmittedOpcodes.remove(clientId);
        transmissionTimestamps.remove(clientId);
        logger.debug("Cleared history for client: {}", clientId);
    }

    /**
     * 마지막 전송 opcode 조회
     */
    public Byte getLastTransmittedOpcode(String clientId) {
        return lastTransmittedOpcodes.get(clientId);
    }

    /**
     * 전체 전송 히스토리 조회
     */
    public Map<String, Byte> getClientTransmissionHistory() {
        return Map.copyOf(lastTransmittedOpcodes);
    }

    /**
     * 마지막 전송 시간 조회
     */
    public Long getLastTransmissionTime(String clientId) {
        return transmissionTimestamps.get(clientId);
    }
}