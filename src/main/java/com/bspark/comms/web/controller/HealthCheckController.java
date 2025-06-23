package com.bspark.comms.web.controller;

import com.bspark.comms.network.server.TcpConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);

    @Autowired
    private TcpConnectionFactory tcpConnectionFactory;

    @GetMapping("/api/v1/status/health-check")
    public String receiveHealthCheck(){
       return "OK";
    }

}
