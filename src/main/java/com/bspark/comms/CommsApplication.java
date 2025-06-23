package com.bspark.comms;

import com.bspark.comms.config.CommsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CommsProperties.class)
public class CommsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommsApplication.class, args);
    }
}