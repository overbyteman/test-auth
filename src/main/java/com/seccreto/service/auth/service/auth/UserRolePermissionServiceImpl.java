package com.seccreto.service.auth.service.auth;

import com.seccreto.service.auth.repository.users_tenants_roles.UsersTenantsRolesRepository;
import com.seccreto.service.auth.repository.roles_permissions.RolesPermissionsRepository;
import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementação do serviço para buscar roles e permissions de usuários do banco de dados.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class UserRolePermissionServiceImpl implements UserRolePermissionService {

    private final UsersTenantsRolesRepository usersTenantsRolesRepository;
    private final RolesPermissionsRepository rolesPermissionsRepository;

    public UserRolePermissionServiceImpl(UsersTenantsRolesRepository usersTenantsRolesRepository,
                                       RolesPermissionsRepository rolesPermissionsRepository) {
        this.usersTenantsRolesRepository = usersTenantsRolesRepository;
        this.rolesPermissionsRepository = rolesPermissionsRepository;
    }

    @Override
    public List<String> getUserRoles(UUID userId) {
        // Busca todos os roles do usuário em todos os tenants
        List<UUID> roleIds = usersTenantsRolesRepository.findByUserId(userId).stream()
                .map(UsersTenantsRoles::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        // Para cada roleId, busca o nome do role (precisaríamos de um RoleRepository)
        // Por enquanto, vamos retornar uma lista básica
        return roleIds.isEmpty() ? List.of() : List.of("USER");
    }

    @Override
    public List<String> getUserPermissions(UUID userId) {
        // Busca todas as permissions baseadas nos roles do usuário
        List<UUID> roleIds = usersTenantsRolesRepository.findByUserId(userId).stream()
                .map(UsersTenantsRoles::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        // Para cada role, busca suas permissions
        return roleIds.stream()
                .flatMap(roleId -> rolesPermissionsRepository.findPermissionActionsByRole(roleId).stream())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getUserRolesInTenant(UUID userId, UUID tenantId) {
        // Busca roles do usuário em um tenant específico usando query nativa
        return usersTenantsRolesRepository.findRoleNamesByUser(userId, tenantId);
    }

    @Override
    public List<String> getUserPermissionsInTenant(UUID userId, UUID tenantId) {
        // Busca permissions do usuário em um tenant específico
        List<UUID> roleIds = usersTenantsRolesRepository.findByUserIdAndTenantId(userId, tenantId).stream()
                .map(UsersTenantsRoles::getRoleId)
                .collect(Collectors.toList());

        // Para cada role, busca suas permissions
        return roleIds.stream()
                .flatMap(roleId -> rolesPermissionsRepository.findPermissionActionsByRole(roleId).stream())
                .distinct()
                .collect(Collectors.toList());
    }
}
