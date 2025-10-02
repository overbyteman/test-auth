package com.seccreto.service.auth.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sistema de Rate Limiting baseado em anotações.
 * 
 * Características:
 * - Rate limiting por usuário autenticado
 * - Rate limiting por IP para usuários não autenticados
 * - Configurável por endpoint
 * - Cache em memória (pode ser substituído por Redis)
 */
@Aspect
@Component
public class RateLimitingAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingAspect.class);
    
    // Cache em memória para rate limiting (em produção, usar Redis)
    private final ConcurrentMap<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();

    /**
     * Anotação para aplicar rate limiting
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RateLimit {
        int requests() default 10; // Número de requests permitidos
        int windowMinutes() default 1; // Janela de tempo em minutos
        String message() default "Rate limit exceeded. Try again later.";
    }

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String identifier = getIdentifier();
        String key = joinPoint.getSignature().toShortString() + ":" + identifier;
        
        RateLimitInfo info = rateLimitCache.computeIfAbsent(key, k -> new RateLimitInfo());
        
        synchronized (info) {
            LocalDateTime now = LocalDateTime.now();
            
            // Limpar requests antigos
            info.requests.removeIf(timestamp -> 
                ChronoUnit.MINUTES.between(timestamp, now) >= rateLimit.windowMinutes());
            
            // Verificar se excedeu o limite
            if (info.requests.size() >= rateLimit.requests()) {
                logger.warn("Rate limit exceeded for {}: {} requests in {} minutes", 
                    identifier, info.requests.size(), rateLimit.windowMinutes());
                
                throw new RateLimitExceededException(rateLimit.message());
            }
            
            // Adicionar request atual
            info.requests.add(now);
            
            logger.debug("Rate limit check for {}: {}/{} requests", 
                identifier, info.requests.size(), rateLimit.requests());
        }
        
        return joinPoint.proceed();
    }

    private String getIdentifier() {
        // Priorizar usuário autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        
        // Fallback para IP
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = getClientIpAddress(request);
            return "ip:" + ip;
        }
        
        return "unknown";
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Informações de rate limiting por chave
     */
    private static class RateLimitInfo {
        private final java.util.List<LocalDateTime> requests = new java.util.ArrayList<>();
    }

    /**
     * Exceção para rate limit excedido
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
