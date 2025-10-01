package com.seccreto.service.auth.service.permissions;

import com.seccreto.service.auth.model.permissions.Permission;

import java.util.List;
import java.util.Optional;

/**
 * Abstração da camada de serviço para operações de permissão.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 */
public interface PermissionService {
    Permission createPermission(String action, String resource);
    List<Permission> listAllPermissions();
    Optional<Permission> findPermissionById(Long id);
    List<Permission> findPermissionsByAction(String action);
    List<Permission> findPermissionsByResource(String resource);
    List<Permission> findPermissionsByActionAndResource(String action, String resource);
    Optional<Permission> findPermissionByActionAndResourceExact(String action, String resource);
    Permission updatePermission(Long id, String action, String resource);
    boolean deletePermission(Long id);
    boolean existsPermissionById(Long id);
    boolean existsPermissionByActionAndResource(String action, String resource);
    long countPermissions();
    
    // Métodos adicionais para controllers
    List<Permission> searchPermissions(String query);
}
