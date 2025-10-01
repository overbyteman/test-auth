package com.seccreto.service.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do Swagger/OpenAPI para documentação da API
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema MVC - API REST")
                        .description("API REST para gerenciamento de usuários implementada com Spring Boot e padrão MVC")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sistema MVC")
                                .email("contato@sistemamvc.com")
                                .url("https://github.com/sistemamvc"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de Desenvolvimento"),
                        new Server()
                                .url("https://api.sistemamvc.com")
                                .description("Servidor de Produção")
                ));
    }
}

