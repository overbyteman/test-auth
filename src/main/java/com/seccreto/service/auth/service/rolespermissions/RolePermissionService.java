package com.seccreto.service.auth.service.rolespermissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePermissionService {

    RolesPermissions attachPermission(UUID landlordId, UUID roleId, UUID permissionId, UUID policyId, boolean inheritDefaultPolicy);

    RolesPermissions updatePermissionPolicy(UUID landlordId, UUID roleId, UUID permissionId, UUID policyId, boolean inheritDefaultPolicy);

    boolean detachPermission(UUID landlordId, UUID roleId, UUID permissionId);

    Optional<RolesPermissions> findAssociation(UUID landlordId, UUID roleId, UUID permissionId);

    List<RolesPermissions> listRolePermissions(UUID landlordId, UUID roleId);
}
