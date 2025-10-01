package com.seccreto.service.auth.service.users_tenants_roles;

import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;

import java.util.List;
import java.util.Optional;

/**
 * Abstração da camada de serviço para operações de relacionamento user-tenant-role.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 */
public interface UsersTenantsRolesService {
    UsersTenantsRoles assignRoleToUserInTenant(Long userId, Long tenantId, Long roleId);
    UsersTenantsRoles createAssociation(Long userId, Long tenantId, Long roleId);
    List<UsersTenantsRoles> listAllUserTenantRoles();
    Optional<UsersTenantsRoles> findUserTenantRole(Long userId, Long tenantId, Long roleId);
    List<UsersTenantsRoles> findRolesByUser(Long userId);
    List<UsersTenantsRoles> findUsersByTenant(Long tenantId);
    List<UsersTenantsRoles> findUsersByRole(Long roleId);
    List<UsersTenantsRoles> findRolesByUserAndTenant(Long userId, Long tenantId);
    List<UsersTenantsRoles> findUsersByTenantAndRole(Long tenantId, Long roleId);
    boolean removeRoleFromUserInTenant(Long userId, Long tenantId, Long roleId);
    boolean removeAssociation(Long userId, Long tenantId, Long roleId);
    boolean removeAllRolesFromUser(Long userId);
    boolean removeAllUsersFromTenant(Long tenantId);
    boolean removeAllUsersFromRole(Long roleId);
    boolean removeAllRolesFromUserInTenant(Long userId, Long tenantId);
    boolean existsUserTenantRole(Long userId, Long tenantId, Long roleId);
    boolean existsRolesForUser(Long userId);
    boolean existsUsersForTenant(Long tenantId);
    boolean existsUsersForRole(Long roleId);
    boolean existsRolesForUserInTenant(Long userId, Long tenantId);
    long countUserTenantRoles();
    long countAssociations();
    long countRolesByUser(Long userId);
    long countUsersByTenant(Long tenantId);
    long countUsersByRole(Long roleId);
    long countRolesByUserAndTenant(Long userId, Long tenantId);

    // Métodos adicionais para controllers (renomeados para evitar conflito)
    List<String> findRoleNamesByUser(Long userId);
    List<String> findPermissionsByUser(Long userId);
    List<String> findPermissionNamesByUser(Long userId);
    long countPermissionsByUser(Long userId);
}
