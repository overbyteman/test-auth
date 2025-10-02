package com.seccreto.service.auth.service.permissions;

import com.seccreto.service.auth.model.permissions.Permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de permission.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 * Baseado na migração V5.
 */
public interface PermissionService {
    
    // Operações básicas CRUD
    Permission createPermission(String action, String resource);
    List<Permission> listAllPermissions();
    Optional<Permission> findPermissionById(UUID id);
    List<Permission> findPermissionsByAction(String action);
    List<Permission> findPermissionsByResource(String resource);
    Optional<Permission> findPermissionByActionAndResource(String action, String resource);
    Permission updatePermission(UUID id, String action, String resource);
    boolean deletePermission(UUID id);
    boolean existsPermissionById(UUID id);
    boolean existsPermissionByActionAndResource(String action, String resource);
    long countPermissions();
    
    // Operações de busca
    List<Permission> searchPermissions(String query);
    
    // Operações de roles
    List<Object> getPermissionRoles(UUID permissionId);
    long countPermissionRoles(UUID permissionId);
}