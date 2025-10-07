package com.seccreto.service.auth.repository.users_tenants_permissions;

import com.seccreto.service.auth.model.users_tenants_permissions.UsersTenantsPermissions;
import com.seccreto.service.auth.model.users_tenants_permissions.UsersTenantsPermissionsId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para UsersTenantsPermissions
 */
@Repository
public interface UsersTenantsPermissionsRepository extends JpaRepository<UsersTenantsPermissions, UsersTenantsPermissionsId> {

    @EntityGraph(attributePaths = {"permission"})
    List<UsersTenantsPermissions> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"permission"})
    List<UsersTenantsPermissions> findByTenantId(UUID tenantId);

    @EntityGraph(attributePaths = {"permission"})
    List<UsersTenantsPermissions> findByPermissionId(UUID permissionId);

    @EntityGraph(attributePaths = {"permission"})
    List<UsersTenantsPermissions> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    @EntityGraph(attributePaths = {"permission"})
    List<UsersTenantsPermissions> findByUserIdAndPermissionId(UUID userId, UUID permissionId);

    @EntityGraph(attributePaths = {"permission"})
    List<UsersTenantsPermissions> findByTenantIdAndPermissionId(UUID tenantId, UUID permissionId);

    boolean existsByUserIdAndTenantIdAndPermissionId(UUID userId, UUID tenantId, UUID permissionId);

    Optional<UsersTenantsPermissions> findByUserIdAndTenantIdAndPermissionId(UUID userId, UUID tenantId, UUID permissionId);

    void deleteByUserIdAndTenantIdAndPermissionId(UUID userId, UUID tenantId, UUID permissionId);

    void deleteByUserIdAndTenantId(UUID userId, UUID tenantId);

    void deleteByTenantIdAndPermissionId(UUID tenantId, UUID permissionId);

    void deleteByUserIdAndPermissionId(UUID userId, UUID permissionId);

    long countByUserIdAndTenantId(UUID userId, UUID tenantId);

    long countByTenantIdAndPermissionId(UUID tenantId, UUID permissionId);

    long countByUserIdAndPermissionId(UUID userId, UUID permissionId);

    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);

    boolean existsByTenantIdAndPermissionId(UUID tenantId, UUID permissionId);

    boolean existsByUserIdAndPermissionId(UUID userId, UUID permissionId);

    @Query(value = "SELECT COUNT(*) FROM users_tenants_permissions", nativeQuery = true)
    long countAssociations();

    @Query(value = "SELECT DISTINCT p.id FROM users_tenants_permissions utp " +
                   "JOIN permissions p ON utp.permission_id = p.id " +
                   "WHERE utp.user_id = :userId AND utp.tenant_id = :tenantId", nativeQuery = true)
    List<UUID> findPermissionIdsByUserAndTenant(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Query(value = "SELECT DISTINCT p.id FROM users_tenants_permissions utp " +
                   "JOIN permissions p ON utp.permission_id = p.id " +
                   "WHERE utp.user_id = :userId", nativeQuery = true)
    List<UUID> findPermissionIdsByUser(@Param("userId") UUID userId);

    @Query(value = "SELECT p.id, p.action, p.resource FROM permissions p " +
                   "JOIN users_tenants_permissions utp ON p.id = utp.permission_id " +
                   "WHERE utp.user_id = :userId AND utp.tenant_id = :tenantId " +
                   "ORDER BY p.action, p.resource", nativeQuery = true)
    List<Object[]> getUserPermissionsDetails(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    default UsersTenantsPermissions createAssociation(UUID userId, UUID tenantId, UUID permissionId) {
        UsersTenantsPermissions association = UsersTenantsPermissions.createNew(userId, tenantId, permissionId);
        return save(association);
    }

    default void removeAssociation(UUID userId, UUID tenantId, UUID permissionId) {
        UsersTenantsPermissionsId id = new UsersTenantsPermissionsId(userId, tenantId, permissionId);
        deleteById(id);
    }
}
