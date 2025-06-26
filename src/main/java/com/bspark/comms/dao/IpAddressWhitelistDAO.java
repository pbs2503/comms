package com.bspark.comms.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@Repository
public class IpAddressWhitelistDAO {

    private static final Logger logger = LoggerFactory.getLogger(IpAddressWhitelistDAO.class);
    private final DataSource dataSource;

    public IpAddressWhitelistDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 허용된 모든 IP 주소 목록 조회
     */
    public Set<String> getAllowedIps() {
        String sql = "SELECT ip_address FROM tsc_schema.tb_tsc_list";
        Set<String> allowedIps = new HashSet<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                allowedIps.add(rs.getString("ip_address"));
            }

            logger.info("PostgreSQL에서 {} 개의 허용된 IP 주소 로드", allowedIps.size());

        } catch (SQLException e) {
            logger.error("허용된 IP 목록 조회 중 데이터베이스 오류: {}", e.getMessage());
            throw new RuntimeException("화이트리스트 로드 실패", e);
        }

        return allowedIps;
    }

    public boolean isIpAllowed(String ipAddress) {
        String sql = "SELECT 1 FROM tsc_schema.tb_tsc_list WHERE ip_address = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ipAddress);
            try (ResultSet rs = ps.executeQuery()) {
                boolean allowed = rs.next();
                logger.debug("IP address {} is {}", ipAddress, allowed ? "allowed" : "not allowed");
                return allowed;
            }
        } catch (SQLException e) {
            logger.error("Database error while checking IP whitelist for {}: {}", ipAddress, e.getMessage());
            return false; // 안전상 차단
        }
    }

    public void updateStatus(String ipAddress) {
        String sql = "UPDATE tsc_schema.tb_tsc_list " +
                     "SET status = 'online', last_access_time = now() " +
                     "WHERE ip_address = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ipAddress);
            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Status updated for IP: {}", ipAddress);
            } else {
                logger.warn("No rows updated for IP: {} (IP might not exist in whitelist)", ipAddress);
            }
        } catch (SQLException e) {
            logger.error("Database error while updating status for {}: {}", ipAddress, e.getMessage());
        }
    }
}