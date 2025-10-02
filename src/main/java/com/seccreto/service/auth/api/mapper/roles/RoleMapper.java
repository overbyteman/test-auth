package com.seccreto.service.auth.api.mapper.roles;

import com.seccreto.service.auth.api.dto.roles.RoleRequest;
import com.seccreto.service.auth.api.dto.roles.RoleResponse;
import com.seccreto.service.auth.model.roles.Role;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversão entre DTOs e entidades de Role.
 * Implementa o padrão de mapeamento estático para performance.
 */
public final class RoleMapper {
    private RoleMapper() {}

    /**
     * Converte RoleRequest para entidade Role.
     */
    public static Role toEntity(RoleRequest request) {
        if (request == null) {
            return null;
        }
        
        return Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Converte entidade Role para RoleResponse.
     */
    public static RoleResponse toResponse(Role role) {
        if (role == null) {
            return null;
        }
        
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }

    /**
     * Converte lista de entidades Role para lista de RoleResponse.
     */
    public static List<RoleResponse> toResponseList(List<Role> roles) {
        if (roles == null) {
            return null;
        }
        
        return roles.stream()
                .map(RoleMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza entidade Role com dados do RoleRequest.
     */
    public static void updateEntity(Role role, RoleRequest request) {
        if (role == null || request == null) {
            return;
        }
        
        role.setName(request.getName());
        role.setDescription(request.getDescription());
    }
}
