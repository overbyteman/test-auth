package com.seccreto.service.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal que executa o sistema MVC com Spring Boot
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        // Application startup messages moved to application.yml logging configuration
        
        SpringApplication.run(Main.class, args);
    }
}
