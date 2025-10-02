package com.seccreto.service.auth.api.mapper.permissions;

import com.seccreto.service.auth.api.dto.permissions.PermissionRequest;
import com.seccreto.service.auth.api.dto.permissions.PermissionResponse;
import com.seccreto.service.auth.model.permissions.Permission;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversão entre DTOs e entidades de Permission.
 * Implementa o padrão de mapeamento estático para performance.
 */
public final class PermissionMapper {
    private PermissionMapper() {}

    /**
     * Converte PermissionRequest para entidade Permission.
     */
    public static Permission toEntity(PermissionRequest request) {
        if (request == null) {
            return null;
        }
        
        return Permission.builder()
                .action(request.getAction())
                .resource(request.getResource())
                .build();
    }

    /**
     * Converte entidade Permission para PermissionResponse.
     */
    public static PermissionResponse toResponse(Permission permission) {
        if (permission == null) {
            return null;
        }
        
        return PermissionResponse.builder()
                .id(permission.getId())
                .action(permission.getAction())
                .resource(permission.getResource())
                .permissionString(permission.getPermissionString())
                .build();
    }

    /**
     * Converte lista de entidades Permission para lista de PermissionResponse.
     */
    public static List<PermissionResponse> toResponseList(List<Permission> permissions) {
        if (permissions == null) {
            return null;
        }
        
        return permissions.stream()
                .map(PermissionMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza entidade Permission com dados do PermissionRequest.
     */
    public static void updateEntity(Permission permission, PermissionRequest request) {
        if (permission == null || request == null) {
            return;
        }
        
        permission.setAction(request.getAction());
        permission.setResource(request.getResource());
    }
}
