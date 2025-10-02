package com.seccreto.service.auth.repository.roles;

import com.seccreto.service.auth.model.roles.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para Role
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // ========================================
    // QUERIES DERIVADAS AUTOMÁTICAS (JPA)
    // ========================================
    
    Optional<Role> findByName(String name);
    
    List<Role> findByNameContainingIgnoreCase(String name);
    
    boolean existsByName(String name);

    // ========================================
    // QUERIES CUSTOMIZADAS COM @Query
    // ========================================
    
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Role> findByNameLike(@Param("name") String name);
    
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) = LOWER(:name)")
    Optional<Role> findByNameExact(@Param("name") String name);
    
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Role> search(@Param("query") String query);
    
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Role> search(@Param("query") String query, Pageable pageable);

    // ========================================
    // QUERIES DE MÉTRICAS E RELATÓRIOS
    // ========================================
    
    @Query(value = "SELECT r.name, COUNT(utr.user_id) FROM roles r " +
                   "LEFT JOIN users_tenants_roles utr ON r.id = utr.role_id " +
                   "GROUP BY r.id, r.name", nativeQuery = true)
    List<Object[]> getRoleUsageStats();
    
    @Query(value = "SELECT COUNT(DISTINCT utr.user_id) FROM users_tenants_roles utr WHERE utr.role_id = :roleId", nativeQuery = true)
    long countUsersByRole(@Param("roleId") UUID roleId);
    
    // ========================================
    // QUERIES PARA SUBSTITUIR JDBC
    // ========================================
    
    @Query("SELECT p.id, p.action, p.resource FROM Permission p " +
           "JOIN RolesPermissions rp ON p.id = rp.permissionId " +
           "WHERE rp.roleId = :roleId ORDER BY p.action, p.resource")
    List<Object[]> getRolePermissionsDetails(@Param("roleId") UUID roleId);
    
    @Query("SELECT COUNT(rp) > 0 FROM RolesPermissions rp " +
           "JOIN Permission p ON rp.permissionId = p.id " +
           "WHERE rp.roleId = :roleId AND p.action = :action AND p.resource = :resource")
    boolean roleHasPermissionByActionAndResource(@Param("roleId") UUID roleId, 
                                               @Param("action") String action, 
                                               @Param("resource") String resource);
    
    @Query("SELECT COUNT(rp) FROM RolesPermissions rp WHERE rp.roleId = :roleId")
    long countPermissionsByRole(@Param("roleId") UUID roleId);
    
    @Query(value = "SELECT u.id, u.name, u.email, u.is_active FROM users u " +
                   "JOIN users_tenants_roles utr ON u.id = utr.user_id " +
                   "WHERE utr.role_id = :roleId ORDER BY u.name", nativeQuery = true)
    List<Object[]> getRoleUsersDetails(@Param("roleId") UUID roleId);
    
    default Map<String, Long> getRoleDistribution() {
        // Implementação pode ser adicionada se necessário
        return Map.of();
    }
}