package com.signatureapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 📄 Document Signature App - Main Application Entry Point
 * Java 17 + Spring Boot 3.2.x + MySQL
 */
@SpringBootApplication
@EnableAsync
public class SignatureAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignatureAppApplication.class, args);
        System.out.println("""
                ╔═══════════════════════════════════════════════╗
                ║   📄 Document Signature App Started           ║
                ║   Java 17 | Spring Boot 3.2 | MySQL           ║
                ║   Server: http://localhost:8080               ║
                ╚═══════════════════════════════════════════════╝
                """);
    }
}
