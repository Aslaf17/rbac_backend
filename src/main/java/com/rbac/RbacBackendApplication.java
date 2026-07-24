package com.rbac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
@org.springframework.scheduling.annotation.EnableAsync
@org.springframework.scheduling.annotation.Async

@SpringBootApplication
public class RbacBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RbacBackendApplication.class, args);
    }
}
