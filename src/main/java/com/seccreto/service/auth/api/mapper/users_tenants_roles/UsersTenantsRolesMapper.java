package com.seccreto.service.auth.api.mapper.users_tenants_roles;

import com.seccreto.service.auth.api.dto.users_tenants_roles.UsersTenantsRolesRequest;
import com.seccreto.service.auth.api.dto.users_tenants_roles.UsersTenantsRolesResponse;
import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversão entre DTOs e entidades de UsersTenantsRoles.
 * Implementa o padrão de mapeamento estático para performance.
 */
public final class UsersTenantsRolesMapper {
    private UsersTenantsRolesMapper() {}

    /**
     * Converte UsersTenantsRolesRequest para entidade UsersTenantsRoles.
     */
    public static UsersTenantsRoles toEntity(UsersTenantsRolesRequest request) {
        if (request == null) {
            return null;
        }
        
        return UsersTenantsRoles.builder()
                .userId(request.getUserId())
                .tenantId(request.getTenantId())
                .roleId(request.getRoleId())
                .build();
    }

    /**
     * Converte entidade UsersTenantsRoles para UsersTenantsRolesResponse.
     */
    public static UsersTenantsRolesResponse toResponse(UsersTenantsRoles usersTenantsRoles) {
        if (usersTenantsRoles == null) {
            return null;
        }
        
        return UsersTenantsRolesResponse.builder()
                .userId(usersTenantsRoles.getUserId())
                .tenantId(usersTenantsRoles.getTenantId())
                .roleId(usersTenantsRoles.getRoleId())
                .createdAt(usersTenantsRoles.getCreatedAt())
                .build();
    }

    /**
     * Converte lista de entidades UsersTenantsRoles para lista de UsersTenantsRolesResponse.
     */
    public static List<UsersTenantsRolesResponse> toResponseList(List<UsersTenantsRoles> usersTenantsRoles) {
        if (usersTenantsRoles == null) {
            return null;
        }
        
        return usersTenantsRoles.stream()
                .map(UsersTenantsRolesMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza entidade UsersTenantsRoles com dados do UsersTenantsRolesRequest.
     */
    public static void updateEntity(UsersTenantsRoles usersTenantsRoles, UsersTenantsRolesRequest request) {
        if (usersTenantsRoles == null || request == null) {
            return;
        }
        
        usersTenantsRoles.setUserId(request.getUserId());
        usersTenantsRoles.setTenantId(request.getTenantId());
        usersTenantsRoles.setRoleId(request.getRoleId());
    }
}
