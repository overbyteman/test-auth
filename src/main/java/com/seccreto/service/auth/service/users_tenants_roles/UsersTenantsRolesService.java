package com.seccreto.service.auth.service.users_tenants_roles;

import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de users_tenants_roles.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 * Baseado na migração V7.
 */
public interface UsersTenantsRolesService {
    
    // Operações básicas CRUD
    UsersTenantsRoles createUserTenantRole(UUID userId, UUID tenantId, UUID roleId);
    UsersTenantsRoles createAssociation(UUID userId, UUID tenantId, UUID roleId);
    List<UsersTenantsRoles> listAllUserTenantRoles();
    Optional<UsersTenantsRoles> findUserTenantRole(UUID userId, UUID tenantId, UUID roleId);
    List<UsersTenantsRoles> findRolesByUserAndTenant(UUID userId, UUID tenantId);
    List<UsersTenantsRoles> findUsersByTenantAndRole(UUID tenantId, UUID roleId);
    List<UsersTenantsRoles> findTenantsByUserAndRole(UUID userId, UUID roleId);
    boolean deleteUserTenantRole(UUID userId, UUID tenantId, UUID roleId);
    boolean removeAssociation(UUID userId, UUID tenantId, UUID roleId);
    boolean deleteAllRolesByUserAndTenant(UUID userId, UUID tenantId);
    boolean deleteAllUsersByTenantAndRole(UUID tenantId, UUID roleId);
    boolean deleteAllTenantsByUserAndRole(UUID userId, UUID roleId);
    boolean existsUserTenantRole(UUID userId, UUID tenantId, UUID roleId);
    boolean existsRolesByUserAndTenant(UUID userId, UUID tenantId);
    boolean existsUsersByTenantAndRole(UUID tenantId, UUID roleId);
    boolean existsTenantsByUserAndRole(UUID userId, UUID roleId);
    long countUserTenantRoles();
    long countAssociations();
    long countRolesByUserAndTenant(UUID userId, UUID tenantId);
    long countUsersByTenantAndRole(UUID tenantId, UUID roleId);
    long countTenantsByUserAndRole(UUID userId, UUID roleId);
    
    // Operações de busca por usuário
    List<String> findRoleNamesByUser(UUID userId);
    List<String> findPermissionNamesByUser(UUID userId);
    long countRolesByUser(UUID userId);
    long countPermissionsByUser(UUID userId);
    
    // Operações de validação
    boolean userHasRoleInTenant(UUID userId, UUID tenantId, UUID roleId);
    boolean userHasRoleInTenantByRoleName(UUID userId, UUID tenantId, String roleName);
    
    // Operações de busca
    List<Object> getUserTenantRolesDetails(UUID userId, UUID tenantId);
    List<Object> getTenantRoleUsersDetails(UUID tenantId, UUID roleId);
    List<Object> getUserRoleTenantsDetails(UUID userId, UUID roleId);
}