
package com.bspark.comms.network.server;

import com.bspark.comms.config.CommsProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TcpServerService {

    private static final Logger logger = LoggerFactory.getLogger(TcpServerService.class);

    private final AcceptClientLoop acceptClientLoop;
    private final CommsProperties commsProperties;

    @PostConstruct
    public void init() {
        if (commsProperties.getServer().isAutoStart()) {
            startServer();
        }
    }

    public void startServer() {
        int port = commsProperties.getServer().getPort();
        logger.info("Starting TCP server on port {}", port);
        acceptClientLoop.startServer(port);
    }

    public void stopServer() {
        logger.info("Stopping TCP server");
        acceptClientLoop.stopServer();
    }

    @PreDestroy
    public void cleanup() {
        stopServer();
    }
}