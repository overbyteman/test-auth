package com.seccreto.service.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração Web MVC
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SecurityValidationInterceptor securityValidationInterceptor;

    public WebConfig(SecurityValidationInterceptor securityValidationInterceptor) {
        this.securityValidationInterceptor = securityValidationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityValidationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register", 
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}
