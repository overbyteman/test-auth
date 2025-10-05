package com.seccreto.service.auth.service.auth;

import com.seccreto.service.auth.repository.users_tenants_roles.UsersTenantsRolesRepository;
import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementação do serviço para buscar roles e permissions de usuários do banco de dados.
 *
 * NOVA ESTRUTURA:
 * - Busca roles do usuário via UsersTenantsRoles
 * - Busca permissions via relacionamento JPA Role.permissions
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class UserRolePermissionServiceImpl implements UserRolePermissionService {

    private final UsersTenantsRolesRepository usersTenantsRolesRepository;

    public UserRolePermissionServiceImpl(UsersTenantsRolesRepository usersTenantsRolesRepository) {
        this.usersTenantsRolesRepository = usersTenantsRolesRepository;
    }

    @Override
    public List<String> getUserRoles(UUID userId) {
        // Busca todos os roles do usuário em todos os tenants
    return usersTenantsRolesRepository.findByUserId(userId).stream()
        .map(UsersTenantsRoles::getRole)
        .filter(role -> role != null)
        .map(Role::getName)
        .distinct()
        .collect(Collectors.toList());
    }

    @Override
    public List<String> getUserPermissions(UUID userId) {
        // Busca todos os roles do usuário
    return usersTenantsRolesRepository.findByUserId(userId).stream()
        .map(UsersTenantsRoles::getRole)
        .filter(role -> role != null)
                .flatMap(role -> role.getRolePermissions().stream())
                .map(RolesPermissions::getPermission)
        .filter(permission -> permission != null)
                .map(Permission::getPermissionString)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getUserRolesInTenant(UUID userId, UUID tenantId) {
        // Busca roles do usuário em um tenant específico
    return usersTenantsRolesRepository.findByUserIdAndTenantId(userId, tenantId).stream()
        .map(UsersTenantsRoles::getRole)
        .filter(role -> role != null)
        .map(Role::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getUserPermissionsInTenant(UUID userId, UUID tenantId) {
        // Busca permissions do usuário em um tenant específico
    return usersTenantsRolesRepository.findByUserIdAndTenantId(userId, tenantId).stream()
        .map(UsersTenantsRoles::getRole)
        .filter(role -> role != null)
                .flatMap(role -> role.getRolePermissions().stream())
                .map(RolesPermissions::getPermission)
        .filter(permission -> permission != null)
                .map(Permission::getPermissionString)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(UUID userId, String action, String resource) {
        List<String> permissions = getUserPermissions(userId);
        String requiredPermission = action + ":" + resource;
        return permissions.contains(requiredPermission);
    }

    @Override
    public boolean hasPermissionInTenant(UUID userId, UUID tenantId, String action, String resource) {
        List<String> permissions = getUserPermissionsInTenant(userId, tenantId);
        String requiredPermission = action + ":" + resource;
        return permissions.contains(requiredPermission);
    }

    @Override
    public boolean hasRole(UUID userId, String roleName) {
        List<String> roles = getUserRoles(userId);
        return roles.contains(roleName);
    }

    @Override
    public boolean hasRoleInTenant(UUID userId, UUID tenantId, String roleName) {
        List<String> roles = getUserRolesInTenant(userId, tenantId);
        return roles.contains(roleName);
    }

    @Override
    public List<TenantAccess> getUserTenantAccess(UUID userId) {
        Map<UUID, TenantAggregation> aggregation = new LinkedHashMap<>();

        List<UsersTenantsRoles> associations = usersTenantsRolesRepository.findByUserId(userId);
        for (UsersTenantsRoles association : associations) {
            UUID tenantId = association.getTenantId();
            if (tenantId == null) {
                continue;
            }

            TenantAggregation tenantAggregation = aggregation.computeIfAbsent(tenantId, id -> new TenantAggregation());

            if (association.getTenant() != null) {
                tenantAggregation.setTenantNameIfAbsent(association.getTenant().getName());
            }

            Role role = association.getRole();
            if (role != null) {
                tenantAggregation.addRole(role);
            }
        }

        return aggregation.entrySet().stream()
                .map(entry -> entry.getValue().toTenantAccess(entry.getKey()))
                .toList();
    }

    private static final class TenantAggregation {
        private String tenantName;
        private final Map<String, RoleAggregation> roles = new LinkedHashMap<>();

        void setTenantNameIfAbsent(String name) {
            if (this.tenantName == null && name != null && !name.isBlank()) {
                this.tenantName = name;
            }
        }

        void addRole(Role role) {
            if (role == null || role.getName() == null || role.getName().isBlank()) {
                return;
            }

            RoleAggregation roleAggregation = roles.computeIfAbsent(role.getName(), RoleAggregation::new);

            role.getRolePermissions().stream()
                    .map(RolesPermissions::getPermission)
                    .filter(Objects::nonNull)
                    .map(Permission::getPermissionString)
                    .filter(permission -> permission != null && !permission.isBlank())
                    .forEach(roleAggregation::addPermission);
        }

        TenantAccess toTenantAccess(UUID tenantId) {
            List<TenantAccess.TenantRoleAccess> roleAccesses = roles.values().stream()
                    .map(RoleAggregation::toTenantRoleAccess)
                    .toList();
            return new TenantAccess(tenantId, tenantName, roleAccesses);
        }
    }

    private static final class RoleAggregation {
        private final String roleName;
        private final java.util.LinkedHashSet<String> permissions = new java.util.LinkedHashSet<>();

        RoleAggregation(String roleName) {
            this.roleName = roleName;
        }

        void addPermission(String permission) {
            permissions.add(permission);
        }

        TenantAccess.TenantRoleAccess toTenantRoleAccess() {
            List<String> permissionList = permissions.stream().toList();
            return new TenantAccess.TenantRoleAccess(roleName, permissionList);
        }
    }
}
