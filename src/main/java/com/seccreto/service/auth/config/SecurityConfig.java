package com.seccreto.service.auth.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
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
    private final String[] allowedOrigins;
    private final String[] allowedMethods;
    private final String[] allowedHeaders;
    private final boolean swaggerEnabled;
    private final boolean publicHealthEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         PostQuantumPasswordEncoder postQuantumPasswordEncoder,
                         Environment environment,
                         @Value("${app.security.cors.allowed-origins:}") String allowedOrigins,
                         @Value("${app.security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}") String allowedMethods,
                         @Value("${app.security.cors.allowed-headers:Authorization,Content-Type,X-Requested-With,Accept,Origin}") String allowedHeaders,
                         @Value("${app.security.allow-swagger:false}") boolean allowSwagger,
                         @Value("${app.security.public-health-enabled:true}") boolean publicHealthEnabled) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.postQuantumPasswordEncoder = postQuantumPasswordEncoder;
        this.allowedOrigins = parseList(allowedOrigins);
        this.allowedMethods = parseList(allowedMethods);
        this.allowedHeaders = parseList(allowedHeaders);
        boolean devLikeProfile = Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(profile -> profile.equals("dev") || profile.equals("test") || profile.equals("local"));
        this.swaggerEnabled = allowSwagger || devLikeProfile;
        this.publicHealthEnabled = publicHealthEnabled;
    }

    /**
     * Configuração de CORS segura para desenvolvimento e produção
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // CORS para API endpoints
                String[] origins = allowedOrigins.length == 0
                        ? new String[] {"https://yourdomain.com"}
                        : allowedOrigins;
                String[] methods = allowedMethods.length == 0
                        ? new String[] {"GET", "POST"}
                        : allowedMethods;
                String[] headers = allowedHeaders.length == 0
                        ? new String[] {"Authorization", "Content-Type"}
                        : allowedHeaders;

                boolean allowCredentials = Arrays.stream(origins).noneMatch("*"::equals);

                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods(methods)
                        .allowedHeaders(headers)
                        .allowCredentials(allowCredentials)
                        .maxAge(3600);

                if (swaggerEnabled) {
                    registry.addMapping("/swagger-ui/**")
                            .allowedOrigins(origins)
                            .allowedMethods("GET")
                            .allowedHeaders(headers)
                            .allowCredentials(allowCredentials)
                            .maxAge(3600);

                    registry.addMapping("/v3/api-docs/**")
                            .allowedOrigins(origins)
                            .allowedMethods("GET")
                            .allowedHeaders(headers)
                            .allowCredentials(allowCredentials)
                            .maxAge(3600);
                }
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
            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none';"))
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
            )
            .authorizeHttpRequests(authz -> {
                if (swaggerEnabled) {
                    authz.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll();
                } else {
                    authz.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").denyAll();
                }

                authz.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll();

                if (publicHealthEnabled) {
                    authz.requestMatchers(HttpMethod.GET, "/api/dashboard/health").permitAll();
                }

                authz.requestMatchers(HttpMethod.POST,
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/auth/refresh-token"
                ).permitAll();

                authz.anyRequest().authenticated();
            })
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

    private static String[] parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }
}

