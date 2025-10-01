package com.seccreto.service.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal que executa o sistema MVC com Spring Boot
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        System.out.println("🚀 Iniciando Sistema MVC com Spring Boot e Gradle...");
        System.out.println("==================================================");
        System.out.println("🌐 Acesse: http://localhost:8080/users");
        System.out.println("📚 API REST: http://localhost:8080/users/api");
        System.out.println("==================================================");
        
        SpringApplication.run(Main.class, args);
    }
}
