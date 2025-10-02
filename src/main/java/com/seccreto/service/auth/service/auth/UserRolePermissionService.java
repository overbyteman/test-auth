package com.seccreto.service.auth.service.auth;

import java.util.List;
import java.util.UUID;

/**
 * Serviço para buscar roles e permissions de usuários do banco de dados.
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
}
