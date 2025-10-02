package com.seccreto.service.auth.service.jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço para geração e validação de tokens JWT.
 */
public interface JwtService {
    
    /**
     * Gera um token de acesso JWT
     */
    String generateAccessToken(UUID userId, UUID sessionId, UUID tenantId, List<String> roles, List<String> permissions);
    
    /**
     * Gera um token de refresh
     */
    String generateRefreshToken(UUID userId, UUID sessionId);
    
    /**
     * Valida um token JWT
     */
    JwtValidationResult validateToken(String token);
    
    /**
     * Extrai informações do token sem validar assinatura (para tokens expirados)
     */
    JwtTokenInfo extractTokenInfo(String token);
    
    /**
     * Verifica se o token está próximo do vencimento (para refresh automático)
     */
    boolean isTokenNearExpiry(String token, int minutesThreshold);
    
    /**
     * Resultado da validação do token
     */
    record JwtValidationResult(
        boolean valid,
        UUID userId,
        UUID sessionId,
        UUID tenantId,
        List<String> roles,
        List<String> permissions,
        LocalDateTime expiresAt,
        String reason
    ) {}
    
    /**
     * Informações extraídas do token
     */
    record JwtTokenInfo(
        UUID userId,
        UUID sessionId,
        UUID tenantId,
        List<String> roles,
        List<String> permissions,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt
    ) {}
}
