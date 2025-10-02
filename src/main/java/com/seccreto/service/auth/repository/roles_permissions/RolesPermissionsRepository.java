package com.seccreto.service.auth.repository.roles_permissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração de repositório para a entidade RolesPermissions, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface RolesPermissionsRepository {
    RolesPermissions save(RolesPermissions rolesPermissions);
    Optional<RolesPermissions> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    List<RolesPermissions> findByRoleId(UUID roleId);
    List<RolesPermissions> findByPermissionId(UUID permissionId);
    List<RolesPermissions> findAll();
    boolean deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    boolean deleteByRoleId(UUID roleId);
    boolean deleteByPermissionId(UUID permissionId);
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    boolean existsByRoleId(UUID roleId);
    boolean existsByPermissionId(UUID permissionId);
    long count();
    long countByRoleId(UUID roleId);
    long countByPermissionId(UUID permissionId);
    void clear();
}