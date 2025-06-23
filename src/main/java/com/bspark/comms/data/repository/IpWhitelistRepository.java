package com.bspark.comms.data.repository;

import com.bspark.comms.data.entity.IpWhitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class IpWhitelistRepository {
    private static final Logger logger = LoggerFactory.getLogger(IpWhitelistRepository.class);

    // 임시 인메모리 저장소 (실제로는 JPA/MyBatis 등 사용)
    private final Map<String, IpWhitelist> whitelist = new ConcurrentHashMap<>();

    public IpWhitelistRepository() {
        // 기본 허용 IP 추가
        initializeDefaultWhitelist();
    }

    /**
     * IP 허용 여부 확인
     */
    public boolean isIpAllowed(String ipAddress) {
        IpWhitelist entry = whitelist.get(ipAddress);
        return entry != null && entry.isActive();
    }

    /**
     * IP 상태 업데이트
     */
    public void updateStatus(String ipAddress) {
        IpWhitelist entry = whitelist.get(ipAddress);
        if (entry != null) {
            entry.updateLastAccess();
            logger.info("Status updated for IP: {}", ipAddress);
        } else {
            logger.warn("Attempted to update status for non-whitelisted IP: {}", ipAddress);
        }
    }

    /**
     * IP 추가
     */
    public void addIp(String ipAddress, String description) {
        IpWhitelist entry = IpWhitelist.builder()
                .ipAddress(ipAddress)
                .description(description)
                .active(true)
                .createdAt(LocalDateTime.now())
                .lastAccessAt(LocalDateTime.now())
                .build();

        whitelist.put(ipAddress, entry);
        logger.info("IP added to whitelist: {} - {}", ipAddress, description);
    }

    /**
     * IP 제거
     */
    public void removeIp(String ipAddress) {
        IpWhitelist removed = whitelist.remove(ipAddress);
        if (removed != null) {
            logger.info("IP removed from whitelist: {}", ipAddress);
        } else {
            logger.warn("Attempted to remove non-existent IP: {}", ipAddress);
        }
    }

    /**
     * IP 활성화/비활성화
     */
    public void setIpActive(String ipAddress, boolean active) {
        IpWhitelist entry = whitelist.get(ipAddress);
        if (entry != null) {
            entry.setActive(active);
            logger.info("IP {} set to {}", ipAddress, active ? "active" : "inactive");
        } else {
            logger.warn("Attempted to modify non-existent IP: {}", ipAddress);
        }
    }

    /**
     * 모든 화이트리스트 엔트리 조회
     */
    public List<IpWhitelist> findAll() {
        return List.copyOf(whitelist.values());
    }

    /**
     * 활성 IP 목록 조회
     */
    public List<IpWhitelist> findActiveIps() {
        return whitelist.values().stream()
                .filter(IpWhitelist::isActive)
                .toList();
    }

    /**
     * 특정 IP 정보 조회
     */
    public IpWhitelist findByIp(String ipAddress) {
        return whitelist.get(ipAddress);
    }

    /**
     * 기본 화이트리스트 초기화
     */
    private void initializeDefaultWhitelist() {
        addIp("127.0.0.1", "Localhost");
        addIp("10.1.1.20", "Test Client");
        addIp("192.168.1.100", "Internal Client");

        logger.info("Default IP whitelist initialized with {} entries", whitelist.size());
    }

    /**
     * 화이트리스트 통계
     */
    public WhitelistStatistics getStatistics() {
        long activeCount = whitelist.values().stream()
                .filter(IpWhitelist::isActive)
                .count();

        return WhitelistStatistics.builder()
                .totalEntries(whitelist.size())
                .activeEntries((int) activeCount)
                .inactiveEntries(whitelist.size() - (int) activeCount)
                .build();
    }

    /**
     * 화이트리스트 통계
     */
    @lombok.Getter
    @lombok.Builder
    public static class WhitelistStatistics {
        private final int totalEntries;
        private final int activeEntries;
        private final int inactiveEntries;

        @Override
        public String toString() {
            return String.format("WhitelistStatistics{total=%d, active=%d, inactive=%d}",
                    totalEntries, activeEntries, inactiveEntries);
        }
    }
}