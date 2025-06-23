package com.bspark.comms.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class TcpConfig {

    @Value("${tcp.server.host:0.0.0.0}")
    private String address;

    @Value("${tcp.server.port:7070}")
    private int port;

    private static final Logger logger = LoggerFactory.getLogger(TcpConfig.class);

    @PostConstruct
    public void logConfig() {
        logger.info("==================== [TCP CONFIG] ====================");
        logger.info("Binding address {} to the Connection Factory", address);
        logger.info("Binding port {} to the Connection Factory", port);
        logger.info("======================================================");
    }

}
