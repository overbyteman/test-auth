package com.seccreto.service.auth.repository.permissions;

import com.seccreto.service.auth.model.permissions.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para Permission
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    // ========================================
    // QUERIES DERIVADAS AUTOMÁTICAS (JPA)
    // ========================================
    
    List<Permission> findByAction(String action);
    
    List<Permission> findByResource(String resource);
    
    Optional<Permission> findByActionAndResource(String action, String resource);
    
    List<Permission> findByActionContainingIgnoreCase(String action);
    
    List<Permission> findByResourceContainingIgnoreCase(String resource);
    
    boolean existsByActionAndResource(String action, String resource);

    // ========================================
    // QUERIES CUSTOMIZADAS COM @Query
    // ========================================
    
    @Query("SELECT p FROM Permission p WHERE LOWER(p.action) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Permission> search(@Param("query") String query);
    
    @Query("SELECT p FROM Permission p WHERE LOWER(p.action) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Permission> search(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT DISTINCT p.action FROM Permission p ORDER BY p.action")
    List<String> findAllActions();
    
    @Query("SELECT DISTINCT p.resource FROM Permission p ORDER BY p.resource")
    List<String> findAllResources();

    // ========================================
    // QUERIES DE MÉTRICAS E RELATÓRIOS
    // ========================================
    
    @Query("SELECT p.action, COUNT(p) FROM Permission p GROUP BY p.action")
    List<Object[]> getActionDistribution();
    
    @Query("SELECT p.resource, COUNT(p) FROM Permission p GROUP BY p.resource")
    List<Object[]> getResourceDistribution();
    
    @Query("SELECT COUNT(DISTINCT rp.roleId) FROM RolesPermissions rp WHERE rp.permissionId = :permissionId")
    long countRolesByPermission(@Param("permissionId") UUID permissionId);

    // ========================================
    // QUERIES PARA SUBSTITUIR JDBC
    // ========================================
    
    @Query("SELECT r.id, r.name, r.description FROM Role r " +
           "JOIN RolesPermissions rp ON r.id = rp.roleId " +
           "WHERE rp.permissionId = :permissionId ORDER BY r.name")
    List<Object[]> getPermissionRolesDetails(@Param("permissionId") UUID permissionId);
}