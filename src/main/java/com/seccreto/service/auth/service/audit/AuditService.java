package com.seccreto.service.auth.service.audit;

import com.seccreto.service.auth.model.audit.AuditLog;
import com.seccreto.service.auth.repository.audit.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * Serviço de auditoria para registrar ações sensíveis do sistema.
 * 
 * Características:
 * - Logging assíncrono para não impactar performance
 * - Captura automática de contexto (usuário, IP, user-agent)
 * - Diferentes níveis de auditoria
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Registra uma ação de sucesso
     */
    @Async
    public void logSuccess(String action, String resourceType, UUID resourceId, String details) {
        try {
            AuditContext context = getCurrentContext();
            
            AuditLog log = AuditLog.success(
                context.userId,
                context.sessionId,
                action,
                resourceType,
                resourceId,
                details,
                context.ipAddress,
                context.userAgent
            );
            
            auditLogRepository.save(log);
            logger.info("Audit log saved: {} by user {} from {}", action, context.userId, context.ipAddress);
            
        } catch (Exception e) {
            logger.error("Failed to save audit log for action: {}", action, e);
        }
    }

    /**
     * Registra uma ação de falha
     */
    @Async
    public void logFailure(String action, String resourceType, UUID resourceId, 
                          String details, String errorMessage) {
        try {
            AuditContext context = getCurrentContext();
            
            AuditLog log = AuditLog.failure(
                context.userId,
                context.sessionId,
                action,
                resourceType,
                resourceId,
                details,
                errorMessage,
                context.ipAddress,
                context.userAgent
            );
            
            auditLogRepository.save(log);
            logger.warn("Audit failure logged: {} by user {} from {} - Error: {}", 
                action, context.userId, context.ipAddress, errorMessage);
            
        } catch (Exception e) {
            logger.error("Failed to save audit failure log for action: {}", action, e);
        }
    }

    /**
     * Registra ação de sistema
     */
    @Async
    public void logSystem(String action, String details) {
        try {
            AuditLog log = AuditLog.system(action, details);
            auditLogRepository.save(log);
            logger.info("System audit log saved: {}", action);
            
        } catch (Exception e) {
            logger.error("Failed to save system audit log for action: {}", action, e);
        }
    }

    /**
     * Métodos de conveniência para ações comuns
     */
    public void logLogin(UUID userId, boolean success, String errorMessage) {
        if (success) {
            logSuccess("LOGIN", "USER", userId, "User logged in successfully");
        } else {
            logFailure("LOGIN", "USER", userId, "Failed login attempt", errorMessage);
        }
    }

    public void logLogout(UUID userId) {
        logSuccess("LOGOUT", "USER", userId, "User logged out");
    }

    public void logUserCreation(UUID userId, UUID createdUserId) {
        logSuccess("CREATE_USER", "USER", createdUserId, "New user created by " + userId);
    }

    public void logUserUpdate(UUID userId, UUID updatedUserId) {
        logSuccess("UPDATE_USER", "USER", updatedUserId, "User updated by " + userId);
    }

    public void logUserDeletion(UUID userId, UUID deletedUserId) {
        logSuccess("DELETE_USER", "USER", deletedUserId, "User deleted by " + userId);
    }

    public void logPasswordChange(UUID userId) {
        logSuccess("CHANGE_PASSWORD", "USER", userId, "Password changed");
    }

    public void logRoleAssignment(UUID userId, UUID targetUserId, String roleName) {
        logSuccess("ASSIGN_ROLE", "USER", targetUserId, 
            "Role '" + roleName + "' assigned by " + userId);
    }

    public void logPermissionGrant(UUID userId, UUID targetUserId, String permission) {
        logSuccess("GRANT_PERMISSION", "USER", targetUserId, 
            "Permission '" + permission + "' granted by " + userId);
    }

    public void logSecurityViolation(String violation, String details) {
        logFailure("SECURITY_VIOLATION", "SYSTEM", null, details, violation);
    }

    /**
     * Captura contexto atual da requisição
     */
    private AuditContext getCurrentContext() {
        AuditContext context = new AuditContext();
        
        // Capturar usuário autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                context.userId = UUID.fromString(auth.getName());
            } catch (IllegalArgumentException e) {
                // Nome não é UUID, usar como string
                logger.debug("Authentication name is not UUID: {}", auth.getName());
            }
        }
        
        // Capturar informações da requisição HTTP
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            context.ipAddress = getClientIpAddress(request);
            context.userAgent = request.getHeader("User-Agent");
            
            // Tentar extrair session ID do header Authorization
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Em uma implementação real, extrairia o session ID do JWT
                // Por enquanto, deixar null
            }
        }
        
        return context;
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
     * Classe interna para contexto de auditoria
     */
    private static class AuditContext {
        UUID userId;
        UUID sessionId;
        String ipAddress;
        String userAgent;
    }
}
