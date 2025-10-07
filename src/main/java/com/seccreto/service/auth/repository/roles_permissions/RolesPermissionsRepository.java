package com.seccreto.service.auth.repository.roles_permissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissionsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolesPermissionsRepository extends JpaRepository<RolesPermissions, RolesPermissionsId> {

    @Query("SELECT rp FROM RolesPermissions rp " +
        "JOIN FETCH rp.role r " +
        "JOIN FETCH rp.permission p " +
        "LEFT JOIN FETCH rp.policy pol " +
        "WHERE r.id = :roleId")
    List<RolesPermissions> findByRoleId(@Param("roleId") UUID roleId);

    @Query("SELECT rp FROM RolesPermissions rp " +
        "JOIN FETCH rp.permission p " +
        "JOIN FETCH rp.role r " +
        "LEFT JOIN FETCH rp.policy pol " +
        "WHERE p.id = :permissionId")
    List<RolesPermissions> findByPermissionId(@Param("permissionId") UUID permissionId);

    @Query("SELECT rp FROM RolesPermissions rp " +
        "JOIN FETCH rp.role r " +
        "JOIN FETCH rp.permission p " +
        "LEFT JOIN FETCH rp.policy pol " +
        "WHERE r.id = :roleId AND p.id = :permissionId")
    Optional<RolesPermissions> findByRoleIdAndPermissionId(@Param("roleId") UUID roleId,
                                   @Param("permissionId") UUID permissionId);

    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    @Modifying
    @Query("DELETE FROM RolesPermissions rp WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId")
    int deleteByRoleIdAndPermissionId(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);
}
