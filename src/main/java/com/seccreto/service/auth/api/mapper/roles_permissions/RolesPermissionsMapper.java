package com.seccreto.service.auth.api.mapper.roles_permissions;

import com.seccreto.service.auth.api.dto.roles_permissions.RolesPermissionsRequest;
import com.seccreto.service.auth.api.dto.roles_permissions.RolesPermissionsResponse;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversão entre DTOs e entidades de RolesPermissions.
 * Implementa o padrão de mapeamento estático para performance.
 */
public final class RolesPermissionsMapper {
    private RolesPermissionsMapper() {}

    /**
     * Converte RolesPermissionsRequest para entidade RolesPermissions.
     */
    public static RolesPermissions toEntity(RolesPermissionsRequest request) {
        if (request == null) {
            return null;
        }
        
        return RolesPermissions.builder()
                .roleId(request.getRoleId())
                .permissionId(request.getPermissionId())
                .build();
    }

    /**
     * Converte entidade RolesPermissions para RolesPermissionsResponse.
     */
    public static RolesPermissionsResponse toResponse(RolesPermissions rolesPermissions) {
        if (rolesPermissions == null) {
            return null;
        }
        
        return RolesPermissionsResponse.builder()
                .roleId(rolesPermissions.getRoleId())
                .permissionId(rolesPermissions.getPermissionId())
                .build();
    }

    /**
     * Converte lista de entidades RolesPermissions para lista de RolesPermissionsResponse.
     */
    public static List<RolesPermissionsResponse> toResponseList(List<RolesPermissions> rolesPermissions) {
        if (rolesPermissions == null) {
            return null;
        }
        
        return rolesPermissions.stream()
                .map(RolesPermissionsMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza entidade RolesPermissions com dados do RolesPermissionsRequest.
     */
    public static void updateEntity(RolesPermissions rolesPermissions, RolesPermissionsRequest request) {
        if (rolesPermissions == null || request == null) {
            return;
        }
        
        rolesPermissions.setRoleId(request.getRoleId());
        rolesPermissions.setPermissionId(request.getPermissionId());
    }
}
