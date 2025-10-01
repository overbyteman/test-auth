package com.seccreto.service.auth.repository.users_tenants_roles;

import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;

import java.util.List;
import java.util.Optional;

/**
 * Abstração de repositório para a entidade UsersTenantsRoles, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface UsersTenantsRolesRepository {
    UsersTenantsRoles save(UsersTenantsRoles usersTenantsRoles);
    Optional<UsersTenantsRoles> findByUserIdAndTenantIdAndRoleId(Long userId, Long tenantId, Long roleId);
    List<UsersTenantsRoles> findByUserId(Long userId);
    List<UsersTenantsRoles> findByTenantId(Long tenantId);
    List<UsersTenantsRoles> findByRoleId(Long roleId);
    List<UsersTenantsRoles> findByUserIdAndTenantId(Long userId, Long tenantId);
    List<UsersTenantsRoles> findByTenantIdAndRoleId(Long tenantId, Long roleId);
    List<UsersTenantsRoles> findAll();
    boolean deleteByUserIdAndTenantIdAndRoleId(Long userId, Long tenantId, Long roleId);
    boolean deleteByUserId(Long userId);
    boolean deleteByTenantId(Long tenantId);
    boolean deleteByRoleId(Long roleId);
    boolean deleteByUserIdAndTenantId(Long userId, Long tenantId);
    boolean existsByUserIdAndTenantIdAndRoleId(Long userId, Long tenantId, Long roleId);
    boolean existsByUserId(Long userId);
    boolean existsByTenantId(Long tenantId);
    boolean existsByRoleId(Long roleId);
    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);
    long count();
    long countByUserId(Long userId);
    long countByTenantId(Long tenantId);
    long countByRoleId(Long roleId);
    long countByUserIdAndTenantId(Long userId, Long tenantId);
    void clear();
}
