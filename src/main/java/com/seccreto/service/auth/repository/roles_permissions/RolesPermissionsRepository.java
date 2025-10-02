package com.seccreto.service.auth.repository.roles_permissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissionsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para RolesPermissions
 */
@Repository
public interface RolesPermissionsRepository extends JpaRepository<RolesPermissions, RolesPermissionsId> {

    // ========================================
    // QUERIES DERIVADAS AUTOMÁTICAS (JPA)
    // ========================================
    
    List<RolesPermissions> findByRoleId(UUID roleId);
    
    List<RolesPermissions> findByPermissionId(UUID permissionId);
    
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    
    // Métodos adicionais para compatibilidade com services
    Optional<RolesPermissions> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    
    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    
    void deleteByRoleId(UUID roleId);
    
    void deleteByPermissionId(UUID permissionId);
    
    boolean existsByRoleId(UUID roleId);
    
    boolean existsByPermissionId(UUID permissionId);
    
    long countByRoleId(UUID roleId);
    
    long countByPermissionId(UUID permissionId);

    // ========================================
    // QUERIES CUSTOMIZADAS COM @Query
    // ========================================
    
    @Query("SELECT COUNT(rp) FROM RolesPermissions rp")
    long countAssociations();
    
    @Query("SELECT DISTINCT rp.permissionId FROM RolesPermissions rp WHERE rp.roleId = :roleId")
    List<UUID> findPermissionIdsByRole(@Param("roleId") UUID roleId);
    
    @Query("SELECT DISTINCT rp.roleId FROM RolesPermissions rp WHERE rp.permissionId = :permissionId")
    List<UUID> findRoleIdsByPermission(@Param("permissionId") UUID permissionId);

    // ========================================
    // QUERIES COM JOIN PARA NOMES
    // ========================================
    
    @Query("SELECT p.action FROM RolesPermissions rp JOIN Permission p ON rp.permissionId = p.id " +
           "WHERE rp.roleId = :roleId")
    List<String> findPermissionActionsByRole(@Param("roleId") UUID roleId);
    
    @Query("SELECT r.name FROM RolesPermissions rp JOIN Role r ON rp.roleId = r.id " +
           "WHERE rp.permissionId = :permissionId")
    List<String> findRoleNamesByPermission(@Param("permissionId") UUID permissionId);
    
    // ========================================
    // QUERIES PARA SUBSTITUIR JDBC
    // ========================================
    
    @Query("SELECT COUNT(rp) > 0 FROM RolesPermissions rp " +
           "JOIN Permission p ON rp.permissionId = p.id " +
           "WHERE rp.roleId = :roleId AND p.action = :action AND p.resource = :resource")
    boolean roleHasPermissionByActionAndResource(@Param("roleId") UUID roleId, 
                                               @Param("action") String action, 
                                               @Param("resource") String resource);
    
    @Query("SELECT p.id, p.action, p.resource FROM Permission p " +
           "JOIN RolesPermissions rp ON p.id = rp.permissionId " +
           "WHERE rp.roleId = :roleId ORDER BY p.action, p.resource")
    List<Object[]> getRolePermissionsDetails(@Param("roleId") UUID roleId);
    
    @Query("SELECT r.id, r.name, r.description FROM Role r " +
           "JOIN RolesPermissions rp ON r.id = rp.roleId " +
           "WHERE rp.permissionId = :permissionId ORDER BY r.name")
    List<Object[]> getPermissionRolesDetails(@Param("permissionId") UUID permissionId);

    // ========================================
    // MÉTODOS DE ASSOCIAÇÃO
    // ========================================
    
    default RolesPermissions createAssociation(UUID roleId, UUID permissionId) {
        RolesPermissions association = RolesPermissions.createNew(roleId, permissionId);
        return save(association);
    }
    
    default void removeAssociation(UUID roleId, UUID permissionId) {
        RolesPermissionsId id = new RolesPermissionsId(roleId, permissionId);
        deleteById(id);
    }
}