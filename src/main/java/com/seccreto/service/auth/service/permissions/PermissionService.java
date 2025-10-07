package com.seccreto.service.auth.service.permissions;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.permissions.PermissionPolicyPresetResponse;
import com.seccreto.service.auth.api.dto.permissions.PermissionResponse;
import com.seccreto.service.auth.api.dto.roles_permissions.RolesPermissionsResponse;
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
    Permission createPermission(UUID landlordId, String action, String resource);
    List<Permission> listPermissions(UUID landlordId);
    Optional<Permission> findPermissionById(UUID landlordId, UUID id);
    List<Permission> findPermissionsByAction(UUID landlordId, String action);
    List<Permission> findPermissionsByResource(UUID landlordId, String resource);
    Optional<Permission> findPermissionByActionAndResource(UUID landlordId, String action, String resource);
    Permission updatePermission(UUID landlordId, UUID id, String action, String resource);
    boolean deletePermission(UUID landlordId, UUID id);
    boolean existsPermissionById(UUID landlordId, UUID id);
    boolean existsPermissionByActionAndResource(UUID landlordId, String action, String resource);
    long countPermissions(UUID landlordId);
    
    // Operações de busca
    List<Permission> searchPermissions(UUID landlordId, String query);
    Pagination<PermissionResponse> searchPermissions(UUID landlordId, SearchQuery searchQuery);
    
    // Operações de roles
    List<RolesPermissionsResponse> getPermissionRoles(UUID landlordId, UUID permissionId);
    long countPermissionRoles(UUID landlordId, UUID permissionId);

    // Catálogo fixo de policies
    List<PermissionPolicyPresetResponse> listPolicyPresets();
}