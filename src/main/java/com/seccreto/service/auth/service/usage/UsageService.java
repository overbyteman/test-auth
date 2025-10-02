package com.seccreto.service.auth.service.usage;

import com.seccreto.service.auth.model.usage.DailyUserUsage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Interface para serviços de métricas de uso do sistema.
 * Baseado nas funções criadas nas migrações V9 e V10.
 */
public interface UsageService {
    
    // Métodos baseados nas funções das migrações
    
    /**
     * Registra um login de usuário (baseado na função record_user_login)
     */
    void recordUserLogin(UUID userId, UUID tenantId, LocalDate usageDate);
    
    /**
     * Registra uma ação de usuário (baseado na função record_user_action)
     */
    void recordUserAction(UUID userId, UUID tenantId, LocalDate usageDate, LocalDateTime actionAt);
    
    /**
     * Obtém métricas de uso diário (baseado na função get_user_daily_usage)
     */
    List<DailyUserUsage> getUserDailyUsage(UUID userId, LocalDate startDate, LocalDate endDate, UUID tenantId);
    
    /**
     * Obtém usuários mais ativos (baseado na função get_top_active_users)
     */
    List<Object> getTopActiveUsers(LocalDate startDate, LocalDate endDate, UUID tenantId, int limit);
    
    /**
     * Limpa dados antigos (baseado na função cleanup_old_usage)
     */
    int cleanupOldUsage(int keepDays);
    
    // Métodos adicionais para controle de uso
    
    /**
     * Obtém permissões de usuário em um tenant (baseado na função get_user_permissions_in_tenant)
     */
    List<Object> getUserPermissionsInTenant(UUID userId, UUID tenantId);
    
    /**
     * Verifica se usuário tem permissão específica (baseado na função user_has_permission_in_tenant)
     */
    boolean userHasPermissionInTenant(UUID userId, UUID tenantId, String action, String resource);
    
    /**
     * Obtém tenants de um usuário com roles (baseado na função get_user_tenants_with_roles)
     */
    List<Object> getUserTenantsWithRoles(UUID userId);
    
    /**
     * Avalia políticas ABAC (baseado na função evaluate_abac_policies)
     */
    List<Object> evaluateAbacPolicies(String action, String resource, String context);
    
    /**
     * Limpa sessões expiradas (baseado na função cleanup_expired_sessions)
     */
    int cleanupExpiredSessions();
}
