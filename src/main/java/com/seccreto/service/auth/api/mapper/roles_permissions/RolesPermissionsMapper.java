package com.seccreto.service.auth.api.mapper.roles_permissions;

import com.seccreto.service.auth.api.dto.roles_permissions.RolesPermissionsResponse;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class RolesPermissionsMapper {

    private RolesPermissionsMapper() {
    }

    public static RolesPermissionsResponse toResponse(RolesPermissions association) {
        if (association == null) {
            return null;
        }

        Permission permission = association.getPermission();
        Policy policy = association.getPolicy();

    Role role = association.getRole();

    return RolesPermissionsResponse.builder()
        .roleId(role != null ? role.getId() : null)
        .roleName(role != null ? role.getName() : null)
        .roleCode(role != null ? role.getCode() : null)
                .permissionId(permission != null ? permission.getId() : null)
                .policyId(policy != null ? policy.getId() : null)
                .permissionAction(permission != null ? permission.getAction() : null)
                .permissionResource(permission != null ? permission.getResource() : null)
                .permissionString(permission != null ? permission.getPermissionString() : null)
                .policyCode(policy != null ? policy.getCode() : null)
                .policyName(policy != null ? policy.getName() : null)
                .policyEffect(policy != null && policy.getEffect() != null ? policy.getEffect().name() : null)
                .build();
    }

    public static List<RolesPermissionsResponse> toResponseList(Collection<RolesPermissions> associations) {
        if (associations == null) {
            return List.of();
        }
        return associations.stream()
                .map(RolesPermissionsMapper::toResponse)
                .collect(Collectors.toList());
    }
}
