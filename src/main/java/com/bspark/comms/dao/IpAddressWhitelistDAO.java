package com.bspark.comms.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class IpAddressWhitelistDAO {

    private static final Logger logger = LoggerFactory.getLogger(IpAddressWhitelistDAO.class);
    private final DataSource dataSource;

    public IpAddressWhitelistDAO(DataSource dataSource) {
        this.dataSource = dataSource;
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