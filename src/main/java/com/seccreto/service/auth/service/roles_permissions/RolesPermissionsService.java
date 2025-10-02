package com.seccreto.service.auth.service.roles_permissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de roles_permissions.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 * Baseado na migração V6.
 */
public interface RolesPermissionsService {
    
    // Operações básicas CRUD
    RolesPermissions createRolePermission(UUID roleId, UUID permissionId);
    RolesPermissions createAssociation(UUID roleId, UUID permissionId);
    List<RolesPermissions> listAllRolePermissions();
    Optional<RolesPermissions> findRolePermission(UUID roleId, UUID permissionId);
    List<RolesPermissions> findPermissionsByRole(UUID roleId);
    List<RolesPermissions> findRolesByPermission(UUID permissionId);
    boolean deleteRolePermission(UUID roleId, UUID permissionId);
    boolean removeAssociation(UUID roleId, UUID permissionId);
    boolean deleteAllPermissionsByRole(UUID roleId);
    boolean deleteAllRolesByPermission(UUID permissionId);
    boolean existsRolePermission(UUID roleId, UUID permissionId);
    boolean existsPermissionsByRole(UUID roleId);
    boolean existsRolesByPermission(UUID permissionId);
    long countRolePermissions();
    long countAssociations();
    long countPermissionsByRole(UUID roleId);
    long countRolesByPermission(UUID permissionId);
    
    // Operações de validação
    boolean roleHasPermission(UUID roleId, UUID permissionId);
    boolean roleHasPermissionByActionAndResource(UUID roleId, String action, String resource);
    
    // Operações de busca
    List<Object> getRolePermissionsDetails(UUID roleId);
    List<Object> getPermissionRolesDetails(UUID permissionId);
}