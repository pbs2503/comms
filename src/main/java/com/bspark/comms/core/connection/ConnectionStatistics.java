package com.bspark.comms.core.connection;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConnectionStatistics {
    private final int activeConnections;
    private final int currentConnections;
    private final int totalConnections;
    private final double connectionSuccessRate;

    @Override
    public String toString() {
        return String.format("ConnectionStatistics{active=%d, current=%d, total=%d, successRate=%.2f%%}",
                activeConnections, currentConnections, totalConnections, connectionSuccessRate);
    }
}