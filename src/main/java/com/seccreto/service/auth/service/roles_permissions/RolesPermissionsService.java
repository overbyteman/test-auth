package com.seccreto.service.auth.service.roles_permissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;

import java.util.List;
import java.util.Optional;

/**
 * Abstração da camada de serviço para operações de relacionamento role-permissão.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 */
public interface RolesPermissionsService {
    RolesPermissions assignPermissionToRole(Long roleId, Long permissionId);
    RolesPermissions createAssociation(Long roleId, Long permissionId);
    List<RolesPermissions> listAllRolePermissions();
    Optional<RolesPermissions> findRolePermission(Long roleId, Long permissionId);
    List<RolesPermissions> findPermissionsByRole(Long roleId);
    List<RolesPermissions> findRolesByPermission(Long permissionId);
    boolean removePermissionFromRole(Long roleId, Long permissionId);
    boolean removeAssociation(Long roleId, Long permissionId);
    boolean removeAllPermissionsFromRole(Long roleId);
    boolean removeAllRolesFromPermission(Long permissionId);
    boolean existsRolePermission(Long roleId, Long permissionId);
    boolean existsPermissionsForRole(Long roleId);
    boolean existsRolesForPermission(Long permissionId);
    long countRolePermissions();
    long countAssociations();
    long countPermissionsByRole(Long roleId);
    long countRolesByPermission(Long permissionId);
}
