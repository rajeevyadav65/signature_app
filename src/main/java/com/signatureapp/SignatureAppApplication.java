package com.signatureapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Document Signature App - Main Application Entry Point
 * Java 17 + Spring Boot 3.2.x + MySQL
 */
@SpringBootApplication
@EnableAsync
public class SignatureAppApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(SignatureAppApplication.class, args);
        Environment environment = context.getEnvironment();
        String port = environment.getProperty(
                "local.server.port",
                environment.getProperty("server.port", "8080"));
        String baseUrl = environment.getProperty("app.base-url", "http://localhost:" + port);

        System.out.println("""
                =========================================
                  Document Signature App Started
                  Java 17 | Spring Boot 3.2 | MySQL
                  Server: %s
                =========================================
                """.formatted(baseUrl));
    }
}
