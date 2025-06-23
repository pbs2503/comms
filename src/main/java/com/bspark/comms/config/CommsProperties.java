
package com.bspark.comms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "comms")
@Getter
@Setter
public class CommsProperties {

    private Server server = new Server();
    private External external = new External();

    @Getter
    @Setter
    public static class Server {
        private int port = 7070;
        private int maxConnections = 100;
        private boolean autoStart = true;
    }

    @Getter
    @Setter
    public static class External {
        private Api api = new Api();

        @Getter
        @Setter
        public static class Api {
            private String baseUrl = "http://localhost:8114";
            private int timeout = 5000;
            private int connectionTimeout = 3000;
            private boolean enabled = true;
        }
    }
}