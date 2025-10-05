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
    
       List<Permission> findByLandlordId(UUID landlordId);

       List<Permission> findByLandlordIdAndAction(UUID landlordId, String action);

       List<Permission> findByLandlordIdAndResource(UUID landlordId, String resource);

       Optional<Permission> findByLandlordIdAndActionAndResource(UUID landlordId, String action, String resource);

       List<Permission> findByLandlordIdAndActionContainingIgnoreCase(UUID landlordId, String action);

       List<Permission> findByLandlordIdAndResourceContainingIgnoreCase(UUID landlordId, String resource);

       boolean existsByLandlordIdAndActionAndResource(UUID landlordId, String action, String resource);

              long countByLandlordId(UUID landlordId);

    // ========================================
    // QUERIES CUSTOMIZADAS COM @Query
    // ========================================
    
    @Query("SELECT p FROM Permission p WHERE p.landlord.id = :landlordId AND (LOWER(p.action) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Permission> search(@Param("landlordId") UUID landlordId, @Param("query") String query);
    
    @Query("SELECT p FROM Permission p WHERE p.landlord.id = :landlordId AND (LOWER(p.action) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.resource) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Permission> search(@Param("landlordId") UUID landlordId, @Param("query") String query, Pageable pageable);
    
    @Query("SELECT DISTINCT p.action FROM Permission p ORDER BY p.action")
    List<String> findAllActions();
    
    @Query("SELECT DISTINCT p.resource FROM Permission p ORDER BY p.resource")
    List<String> findAllResources();

    // ========================================
    // QUERIES DE MÉTRICAS E RELATÓRIOS
    // ========================================
    
              @Query("SELECT p.action, COUNT(p) FROM Permission p WHERE p.landlord.id = :landlordId GROUP BY p.action")
              List<Object[]> getActionDistribution(@Param("landlordId") UUID landlordId);

              @Query("SELECT p.resource, COUNT(p) FROM Permission p WHERE p.landlord.id = :landlordId GROUP BY p.resource")
              List<Object[]> getResourceDistribution(@Param("landlordId") UUID landlordId);

              @Query("SELECT COUNT(DISTINCT rp.role.id) FROM RolesPermissions rp WHERE rp.permission.id = :permissionId")
              long countRolesByPermission(@Param("permissionId") UUID permissionId);

    // ========================================
    // QUERIES PARA SUBSTITUIR JDBC
    // ========================================
    
    @Query("SELECT r.id, r.name, r.description, rp.policy.id FROM RolesPermissions rp " +
           "JOIN rp.role r WHERE rp.permission.id = :permissionId ORDER BY r.name")
    List<Object[]> getPermissionRolesDetails(@Param("permissionId") UUID permissionId);
}