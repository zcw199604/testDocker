package com.example.bizservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Biz service application entrypoint.
 */
@SpringBootApplication
public class BizServiceApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(BizServiceApplication.class, args);
    }
}
