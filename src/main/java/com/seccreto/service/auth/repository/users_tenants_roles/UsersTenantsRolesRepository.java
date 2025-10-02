package com.seccreto.service.auth.repository.users_tenants_roles;

import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração de repositório para a entidade UsersTenantsRoles, permitindo trocar implementação (in-memory, JPA, etc.).
 * Baseado na migração V7.
 */
public interface UsersTenantsRolesRepository {
    UsersTenantsRoles save(UsersTenantsRoles usersTenantsRoles);
    Optional<UsersTenantsRoles> findByUserIdAndTenantIdAndRoleId(UUID userId, UUID tenantId, UUID roleId);
    List<UsersTenantsRoles> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    List<UsersTenantsRoles> findByTenantIdAndRoleId(UUID tenantId, UUID roleId);
    List<UsersTenantsRoles> findByUserIdAndRoleId(UUID userId, UUID roleId);
    List<UsersTenantsRoles> findAll();
    boolean deleteByUserIdAndTenantIdAndRoleId(UUID userId, UUID tenantId, UUID roleId);
    boolean deleteByUserIdAndTenantId(UUID userId, UUID tenantId);
    boolean deleteByTenantIdAndRoleId(UUID tenantId, UUID roleId);
    boolean deleteByUserIdAndRoleId(UUID userId, UUID roleId);
    boolean existsByUserIdAndTenantIdAndRoleId(UUID userId, UUID tenantId, UUID roleId);
    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
    boolean existsByTenantIdAndRoleId(UUID tenantId, UUID roleId);
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
    long count();
    long countByUserIdAndTenantId(UUID userId, UUID tenantId);
    long countByTenantIdAndRoleId(UUID tenantId, UUID roleId);
    long countByUserIdAndRoleId(UUID userId, UUID roleId);
    void clear();
}