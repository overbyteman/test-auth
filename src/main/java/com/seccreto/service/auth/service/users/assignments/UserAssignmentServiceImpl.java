package com.seccreto.service.auth.service.users.assignments;

import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.repository.roles.RoleRepository;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.tenants.TenantService;
import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.users_tenants_permissions.UsersTenantsPermissionsService;
import com.seccreto.service.auth.service.users_tenants_roles.UsersTenantsRolesService;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Serviço orquestrador que garante a atribuição consistente de roles e permissões
 * para usuários dentro de um tenant específico. Responsável por propagar permissões
 * derivadas de roles e evitar duplicidades.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class UserAssignmentServiceImpl implements UserAssignmentService {

    private final UsersTenantsRolesService usersTenantsRolesService;
    private final UsersTenantsPermissionsService usersTenantsPermissionsService;
    private final UserService userService;
    private final TenantService tenantService;
    private final RoleRepository roleRepository;

    public UserAssignmentServiceImpl(UsersTenantsRolesService usersTenantsRolesService,
                                     UsersTenantsPermissionsService usersTenantsPermissionsService,
                                     UserService userService,
                                     TenantService tenantService,
                                     RoleRepository roleRepository) {
        this.usersTenantsRolesService = usersTenantsRolesService;
        this.usersTenantsPermissionsService = usersTenantsPermissionsService;
        this.userService = userService;
        this.tenantService = tenantService;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "user_assignment.assign_roles", description = "Tempo para atribuir roles a um usuário")
    public AssignmentResult assignRoles(UUID userId, UUID tenantId, List<UUID> roleIds) {
        List<UUID> sanitizedRoleIds = normalizeIds(roleIds);
        if (sanitizedRoleIds.isEmpty()) {
            throw new ValidationException("Lista de roles não pode ser vazia");
        }

        User user = requireUser(userId);
        Tenant tenant = requireTenant(tenantId);

        RoleAssignmentResult roleResult = assignRolesInternal(user, tenant, sanitizedRoleIds);
        PermissionAssignmentResult propagated = assignPermissionsInternal(user, tenant, roleResult.derivedPermissionIds(), true);

        return new AssignmentResult(
                user.getId(),
                tenant.getId(),
                roleResult.requestedRoleIds(),
                roleResult.newlyAssignedRoleIds(),
                roleResult.alreadyAssignedRoleIds(),
                List.of(),
                propagated.newlyAssignedPermissionIds(),
                propagated.alreadyAssignedPermissionIds(),
                propagated.propagatedPermissionIds()
        );
    }

    private RoleAssignmentResult assignRolesInternal(User user, Tenant tenant, List<UUID> roleIds) {
        UUID landlordId = requireLandlordId(tenant);

        List<UUID> newlyAssignedRoles = new ArrayList<>();
        List<UUID> alreadyAssignedRoles = new ArrayList<>();
        Set<UUID> derivedPermissionIds = new LinkedHashSet<>();

        for (UUID roleId : roleIds) {
            Role role = requireRoleWithPermissions(landlordId, roleId);
            boolean alreadyAssigned = usersTenantsRolesService.existsUserTenantRole(user.getId(), tenant.getId(), roleId);
            if (alreadyAssigned) {
                alreadyAssignedRoles.add(roleId);
            } else {
                usersTenantsRolesService.createAssociation(user.getId(), tenant.getId(), roleId);
                newlyAssignedRoles.add(roleId);
            }

            if (role.getRolePermissions() != null) {
                for (RolesPermissions rolesPermission : role.getRolePermissions()) {
                    if (rolesPermission.getPermission() != null) {
                        derivedPermissionIds.add(rolesPermission.getPermission().getId());
                    }
                }
            }
        }

        return new RoleAssignmentResult(
                List.copyOf(roleIds),
                List.copyOf(newlyAssignedRoles),
                List.copyOf(alreadyAssignedRoles),
                List.copyOf(derivedPermissionIds)
        );
    }

    private PermissionAssignmentResult assignPermissionsInternal(User user, Tenant tenant, Collection<UUID> permissionIds, boolean propagatedFromRoles) {
        List<UUID> normalized = normalizeIds(permissionIds);
        if (normalized.isEmpty()) {
            return PermissionAssignmentResult.empty();
        }

        List<UUID> requested = List.copyOf(normalized);
        List<UUID> newlyAssigned = new ArrayList<>();
        List<UUID> alreadyAssigned = new ArrayList<>();

        for (UUID permissionId : normalized) {
            boolean already = usersTenantsPermissionsService.existsUserTenantPermission(user.getId(), tenant.getId(), permissionId);
            usersTenantsPermissionsService.createUserTenantPermission(user.getId(), tenant.getId(), permissionId);
            if (already) {
                alreadyAssigned.add(permissionId);
            } else {
                newlyAssigned.add(permissionId);
            }
        }

    List<UUID> propagated = propagatedFromRoles ? List.copyOf(newlyAssigned) : List.of();

        return new PermissionAssignmentResult(
                requested,
                List.copyOf(newlyAssigned),
                List.copyOf(alreadyAssigned),
                propagated
        );
    }

    private User requireUser(UUID userId) {
        return userService.findUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + userId));
    }

    private Tenant requireTenant(UUID tenantId) {
        return tenantService.findTenantById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + tenantId));
    }

    private UUID requireLandlordId(Tenant tenant) {
        if (tenant.getLandlord() == null) {
            throw new ValidationException("Tenant informado não possui landlord associado");
        }
        return tenant.getLandlord().getId();
    }

    private Role requireRoleWithPermissions(UUID landlordId, UUID roleId) {
        Role role = roleRepository.findByIdWithTenantAndPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrado com ID: " + roleId));
        if (role.getLandlord() == null || !Objects.equals(role.getLandlord().getId(), landlordId)) {
            throw new ValidationException("Role não pertence ao mesmo landlord do tenant informado");
        }
        return role;
    }

    private List<UUID> normalizeIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<UUID> unique = new LinkedHashSet<>();
        for (UUID id : ids) {
            if (id != null) {
                unique.add(id);
            }
        }
        return List.copyOf(unique);
    }

    private record RoleAssignmentResult(
            List<UUID> requestedRoleIds,
            List<UUID> newlyAssignedRoleIds,
            List<UUID> alreadyAssignedRoleIds,
            List<UUID> derivedPermissionIds
    ) { }

    private record PermissionAssignmentResult(
            List<UUID> requestedPermissionIds,
            List<UUID> newlyAssignedPermissionIds,
            List<UUID> alreadyAssignedPermissionIds,
            List<UUID> propagatedPermissionIds
    ) {
        static PermissionAssignmentResult empty() {
            return new PermissionAssignmentResult(List.of(), List.of(), List.of(), List.of());
        }
    }
}
