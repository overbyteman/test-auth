package com.seccreto.service.auth.config;

import com.seccreto.service.auth.domain.security.InputSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Enumeration;

/**
 * Interceptor para validação de segurança de entrada
 * Aplica validação automática em todos os parâmetros de requisição
 */
@Component
public class SecurityValidationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SecurityValidationInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Validar parâmetros da query string
            validateQueryParameters(request);
            
            // Validar headers (apenas alguns específicos)
            validateHeaders(request);
            
            return true;
        } catch (Exception e) {
            logger.warn("Security validation failed for request: {} - {}", request.getRequestURI(), e.getMessage());
            // Re-throw para ser tratado pelo GlobalExceptionHandler
            throw e;
        }
    }

    private void validateQueryParameters(HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();
        
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            
            if (paramValues != null) {
                for (String paramValue : paramValues) {
                    if (paramValue != null && !paramValue.trim().isEmpty()) {
                        InputSanitizer.validateInput(paramValue);
                    }
                }
            }
        }
    }

    private void validateHeaders(HttpServletRequest request) {
        // Validar apenas headers específicos que podem conter dados do usuário
        String[] headersToValidate = {
            "User-Agent", "Referer", "X-Forwarded-For", "X-Real-IP"
        };
        
        for (String headerName : headersToValidate) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && !headerValue.trim().isEmpty()) {
                // Aplicar validação mais leniente para headers
                if (InputSanitizer.containsSqlInjection(headerValue)) {
                    InputSanitizer.validateInput(headerValue);
                }
            }
        }
    }
}
