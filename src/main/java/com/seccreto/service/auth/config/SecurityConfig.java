package com.seccreto.service.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração de segurança e CORS para aplicações sênior.
 * 
 * Características:
 * - Configuração de CORS
 * - Headers de segurança
 * - Configuração de recursos estáticos
 * - Spring Security com endpoints públicos
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PostQuantumPasswordEncoder postQuantumPasswordEncoder;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         PostQuantumPasswordEncoder postQuantumPasswordEncoder) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.postQuantumPasswordEncoder = postQuantumPasswordEncoder;
    }

    /**
     * Configuração de CORS segura para desenvolvimento e produção
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                            "http://localhost:3000",
                            "http://localhost:8080",
                            "https://yourdomain.com"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders(
                            "Authorization",
                            "Content-Type",
                            "X-Requested-With",
                            "Accept",
                            "Origin"
                        )
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }

    /**
     * Configuração do Spring Security
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Endpoints públicos
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/forgot-password").permitAll()
                .requestMatchers("/api/auth/reset-password").permitAll()
                .requestMatchers("/api/auth/refresh-token").permitAll()
                .requestMatchers("/api/dashboard/health").permitAll()
                .requestMatchers("/api/*/health").permitAll()
                // Todos os outros endpoints requerem autenticação
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(httpBasic -> httpBasic.disable());
        
        return http.build();
    }

    /**
     * Encoder de senhas pós-quântico usando Argon2id
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return postQuantumPasswordEncoder;
    }
}

