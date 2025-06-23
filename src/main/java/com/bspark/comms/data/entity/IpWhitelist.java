package com.bspark.comms.data.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IpWhitelist {
    private final String ipAddress;
    private final String description;

    @Setter
    private boolean active;

    private final LocalDateTime createdAt;
    private volatile LocalDateTime lastAccessAt;

    /**
     * 마지막 접근 시간 업데이트
     */
    public void updateLastAccess() {
        this.lastAccessAt = LocalDateTime.now();
    }

    /**
     * 생성 후 경과 시간 (일 단위)
     */
    public long getDaysSinceCreated() {
        return java.time.temporal.ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    }

    /**
     * 마지막 접근 후 경과 시간 (시간 단위)
     */
    public long getHoursSinceLastAccess() {
        return java.time.temporal.ChronoUnit.HOURS.between(lastAccessAt, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return String.format("IpWhitelist{ip='%s', active=%s, description='%s', lastAccess=%s}",
                ipAddress, active, description, lastAccessAt);
    }
}