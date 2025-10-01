package com.seccreto.service.auth.repository.roles_permissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Abstração de repositório para a entidade RolesPermissions, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface RolesPermissionsRepository {
    RolesPermissions save(RolesPermissions rolesPermissions);
    Optional<RolesPermissions> findByRoleIdAndPermissionId(Long roleId, Long permissionId);
    List<RolesPermissions> findByRoleId(Long roleId);
    List<RolesPermissions> findByPermissionId(Long permissionId);
    List<RolesPermissions> findAll();
    boolean deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
    boolean deleteByRoleId(Long roleId);
    boolean deleteByPermissionId(Long permissionId);
    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
    boolean existsByRoleId(Long roleId);
    boolean existsByPermissionId(Long permissionId);
    long count();
    long countByRoleId(Long roleId);
    long countByPermissionId(Long permissionId);
    void clear();
}
