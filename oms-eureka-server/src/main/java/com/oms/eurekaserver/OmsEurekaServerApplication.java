package com.oms.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class OmsEurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmsEurekaServerApplication.class, args);
    }

}
