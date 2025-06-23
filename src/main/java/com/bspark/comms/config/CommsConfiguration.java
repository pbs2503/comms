package com.bspark.comms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class CommsConfiguration {

    @Value("${comms.external.api.timeout:5000}")
    private int timeoutMillis;

    @Value("${comms.external.api.connection-timeout:3000}")
    private int connectionTimeoutMillis;

    @Bean(name = "tcpServerExecutor")
    @Primary
    public ExecutorService tcpServerExecutor() {
        return Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "tcp-server-pool");
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }

    @Bean
    @Primary
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }
}