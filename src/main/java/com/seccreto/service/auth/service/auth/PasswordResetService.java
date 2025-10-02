package com.seccreto.service.auth.service.auth;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço para gerenciar tokens de reset de senha de forma segura
 */
@Service
public class PasswordResetService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_MINUTES = 15; // 15 minutos
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, PasswordResetToken> activeTokens = new ConcurrentHashMap<>();
    
    @Value("${app.password-reset.enabled:true}")
    private boolean passwordResetEnabled;
    
    /**
     * Gera um token seguro para reset de senha
     */
    public String generateResetToken(UUID userId) {
        if (!passwordResetEnabled) {
            throw new IllegalStateException("Reset de senha está desabilitado");
        }
        
        // Invalidar tokens anteriores do usuário
        invalidateUserTokens(userId);
        
        // Gerar novo token
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Armazenar token com expiração
        PasswordResetToken resetToken = new PasswordResetToken(
            userId, 
            LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES)
        );
        
        activeTokens.put(token, resetToken);
        
        logger.info("Token de reset gerado para usuário: {}", userId);
        return token;
    }
    
    /**
     * Valida um token de reset de senha
     */
    public UUID validateResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token é obrigatório");
        }
        
        PasswordResetToken resetToken = activeTokens.get(token);
        
        if (resetToken == null) {
            logger.warn("Tentativa de uso de token inválido");
            throw new IllegalArgumentException("Token inválido ou expirado");
        }
        
        if (resetToken.isExpired()) {
            activeTokens.remove(token);
            logger.warn("Tentativa de uso de token expirado para usuário: {}", resetToken.getUserId());
            throw new IllegalArgumentException("Token expirado");
        }
        
        return resetToken.getUserId();
    }
    
    /**
     * Invalida um token após o uso
     */
    public void invalidateToken(String token) {
        PasswordResetToken removed = activeTokens.remove(token);
        if (removed != null) {
            logger.info("Token invalidado para usuário: {}", removed.getUserId());
        }
    }
    
    /**
     * Invalida todos os tokens de um usuário
     */
    public void invalidateUserTokens(UUID userId) {
        activeTokens.entrySet().removeIf(entry -> 
            entry.getValue().getUserId().equals(userId)
        );
        logger.info("Todos os tokens invalidados para usuário: {}", userId);
    }
    
    /**
     * Limpa tokens expirados (deve ser chamado periodicamente)
     */
    public void cleanupExpiredTokens() {
        int removedCount = 0;
        var iterator = activeTokens.entrySet().iterator();
        
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("Removidos {} tokens expirados", removedCount);
        }
    }
    
    /**
     * Classe interna para representar um token de reset
     */
    private static class PasswordResetToken {
        private final UUID userId;
        private final LocalDateTime expiresAt;
        
        public PasswordResetToken(UUID userId, LocalDateTime expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
        
        public UUID getUserId() {
            return userId;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
