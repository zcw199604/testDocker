package com.example.uiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ui service application entrypoint.
 */
@SpringBootApplication
public class UiServiceApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(UiServiceApplication.class, args);
    }
}
