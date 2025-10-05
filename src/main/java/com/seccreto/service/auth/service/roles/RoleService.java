package com.seccreto.service.auth.service.roles;

import com.seccreto.service.auth.model.roles.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de role.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 * Baseado na migração V4.
 */
public interface RoleService {
    
    // Operações básicas CRUD
    Role createRole(UUID tenantId, String code, String name, String description);
    List<Role> listRoles(UUID tenantId);
    Optional<Role> findRoleById(UUID tenantId, UUID id);
    Optional<Role> findRoleByCode(UUID tenantId, String code);
    Role updateRole(UUID tenantId, UUID id, String name, String description);
    boolean deleteRole(UUID tenantId, UUID id);
    boolean existsRoleById(UUID tenantId, UUID id);
    boolean existsRoleByCode(UUID tenantId, String code);
    long countRoles(UUID tenantId);
    
    // Operações de busca
    List<Role> searchRoles(UUID tenantId, String query);
    
    // Operações de permissões
    List<Object> getRolePermissions(UUID tenantId, UUID roleId);
    boolean roleHasPermission(UUID tenantId, UUID roleId, String action, String resource);
    long countRolePermissions(UUID tenantId, UUID roleId);
    
    // Operações de usuários
    List<Object> getRoleUsers(UUID tenantId, UUID roleId);
    long countRoleUsers(UUID tenantId, UUID roleId);
}