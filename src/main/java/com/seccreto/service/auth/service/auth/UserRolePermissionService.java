package com.seccreto.service.auth.service.auth;

import java.util.List;
import java.util.UUID;

/**
 * Serviço para buscar roles e permissions de usuários do banco de dados.
 *
 * ESTRUTURA NORMALIZADA:
 * - Usa tabelas pivot para relacionamentos
 * - Otimizado para queries via índices
 */
public interface UserRolePermissionService {
    
    /**
     * Busca todos os roles de um usuário
     */
    List<String> getUserRoles(UUID userId);
    
    /**
     * Busca todas as permissions de um usuário (baseado em seus roles)
     */
    List<String> getUserPermissions(UUID userId);
    
    /**
     * Busca roles de um usuário em um tenant específico
     */
    List<String> getUserRolesInTenant(UUID userId, UUID tenantId);
    
    /**
     * Busca permissions de um usuário em um tenant específico
     */
    List<String> getUserPermissionsInTenant(UUID userId, UUID tenantId);

    /**
     * Verifica se usuário tem uma permission específica
     */
    boolean hasPermission(UUID userId, String action, String resource);

    /**
     * Retorna o mapeamento completo de tenants, roles e permissions do usuário
     */
    List<TenantAccess> getUserTenantAccess(UUID userId);

    /**
     * Verifica se usuário tem uma permission específica em um tenant
     */
    boolean hasPermissionInTenant(UUID userId, UUID tenantId, String action, String resource);

    /**
     * Verifica se usuário tem um role específico
     */
    boolean hasRole(UUID userId, String roleName);

    /**
     * Verifica se usuário tem um role específico em um tenant
     */
    boolean hasRoleInTenant(UUID userId, UUID tenantId, String roleName);
}
