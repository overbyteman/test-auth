package com.seccreto.service.auth.service.users_tenants_permissions;

import com.seccreto.service.auth.model.users_tenants_permissions.UsersTenantsPermissions;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de users_tenants_permissions.
 * Mantém separação de responsabilidades e permite que controladores dependam apenas de uma interface.
 */
public interface UsersTenantsPermissionsService {

    UsersTenantsPermissions createUserTenantPermission(UUID userId, UUID tenantId, UUID permissionId);

    List<UsersTenantsPermissions> assignPermissions(UUID userId, UUID tenantId, Collection<UUID> permissionIds);

    Optional<UsersTenantsPermissions> findUserTenantPermission(UUID userId, UUID tenantId, UUID permissionId);

    List<UsersTenantsPermissions> findPermissionsByUserAndTenant(UUID userId, UUID tenantId);

    List<UsersTenantsPermissions> findPermissionsByUser(UUID userId);

    boolean removeUserTenantPermission(UUID userId, UUID tenantId, UUID permissionId);

    boolean removePermissions(UUID userId, UUID tenantId, Collection<UUID> permissionIds);

    boolean deleteAllPermissionsByUserAndTenant(UUID userId, UUID tenantId);

    boolean existsUserTenantPermission(UUID userId, UUID tenantId, UUID permissionId);

    boolean existsPermissionsByUserAndTenant(UUID userId, UUID tenantId);

    long countPermissionsByUserAndTenant(UUID userId, UUID tenantId);

    List<Object> getUserTenantPermissionsDetails(UUID userId, UUID tenantId);

    List<UUID> getPermissionIdsByUserAndTenant(UUID userId, UUID tenantId);

    List<UUID> getPermissionIdsByUser(UUID userId);
}
