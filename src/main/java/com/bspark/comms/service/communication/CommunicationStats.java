package com.bspark.comms.service.communication;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunicationStats {
    private final long totalMessagesSent;
    private final long totalBroadcastsSent;
    private final long totalGroupMessagesSent;
    private final int activeConnections;
    private final int requestListSize;

    @Override
    public String toString() {
        return String.format("CommunicationStats{messages=%d, broadcasts=%d, groups=%d, connections=%d, requests=%d}",
                totalMessagesSent, totalBroadcastsSent, totalGroupMessagesSent, activeConnections, requestListSize);
    }
}