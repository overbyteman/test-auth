package com.seccreto.service.auth.repository.roles;

import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.tenants.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para Role com otimizações de performance
 *
 * ESTRUTURA NORMALIZADA:
 * - Roles pertencem a Tenants (FK tenant_id)
 * - Nome único por tenant (UNIQUE constraint)
 * - Tabela pivot roles_permissions para N:N com Permission
 * - EntityGraph para evitar N+1 queries
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // ========================================
    // QUERIES DERIVADAS COM ÍNDICES
    // ========================================
    
    /**
     * Busca role por nome e tenant (usa índice UNIQUE idx_roles_name_tenant)
     */
    Optional<Role> findByNameAndTenant(String name, Tenant tenant);

       Optional<Role> findByNameAndTenantId(String name, UUID tenantId);

    /**
     * Verifica existência (usa índice UNIQUE, mais rápido que SELECT *)
     */
    boolean existsByNameAndTenant(String name, Tenant tenant);

       boolean existsByNameAndTenantId(String name, UUID tenantId);

       Optional<Role> findByCodeAndTenant(String code, Tenant tenant);

       Optional<Role> findByCodeAndTenantId(String code, UUID tenantId);

       boolean existsByCodeAndTenant(String code, Tenant tenant);

       boolean existsByCodeAndTenantId(String code, UUID tenantId);

    /**
     * Lista roles por tenant (usa índice FK tenant_id)
     */
    List<Role> findByTenant(Tenant tenant);

    /**
     * Lista roles por tenant ID (usa índice FK tenant_id)
     */
    List<Role> findByTenantId(UUID tenantId);

       /**
        * Busca case-insensitive dentro de um tenant específico (usa índice funcional se criado)
        */
       List<Role> findByNameContainingIgnoreCaseAndTenantId(String name, UUID tenantId);

    // ========================================
    // QUERIES OTIMIZADAS COM ENTITYGRAPH (evita N+1)
    // ========================================

    /**
     * Busca role com permissions em uma única query (JOIN FETCH)
     * Evita N+1 problem
     */
              @EntityGraph(attributePaths = {"rolePermissions", "rolePermissions.permission", "rolePermissions.policy"})
    @Query("SELECT r FROM Role r WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") UUID id);

    /**
     * Busca role com tenant em uma única query (JOIN FETCH)
     */
       @EntityGraph(attributePaths = {"tenant"})
    @Query("SELECT r FROM Role r WHERE r.id = :id")
    Optional<Role> findByIdWithTenant(@Param("id") UUID id);

    /**
     * Busca role com tenant E permissions em uma única query
     */
              @EntityGraph(attributePaths = {"tenant", "rolePermissions", "rolePermissions.permission", "rolePermissions.policy"})
    @Query("SELECT r FROM Role r WHERE r.id = :id")
    Optional<Role> findByIdWithTenantAndPermissions(@Param("id") UUID id);

    // ========================================
    // QUERIES DE BUSCA OTIMIZADAS
    // ========================================
    
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Role> findByNameLike(@Param("name") String name);
    
    /**
     * Busca full-text em nome e descrição (usa índices se existirem)
     */
    @Query("SELECT r FROM Role r WHERE r.tenant.id = :tenantId AND (" +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Role> search(@Param("tenantId") UUID tenantId, @Param("query") String query);
    
    /**
     * Busca paginada (usa índices + LIMIT/OFFSET otimizado)
     */
    @Query("SELECT r FROM Role r WHERE r.tenant.id = :tenantId AND (" +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Role> search(@Param("tenantId") UUID tenantId, @Param("query") String query, Pageable pageable);

    // ========================================
    // QUERIES AGREGADAS (usa tabela pivot)
    // ========================================
    
    /**
     * Estatísticas de uso de roles via pivot table users_tenants_roles
     * Query nativa otimizada com GROUP BY
     */
    @Query(value = "SELECT r.name, COUNT(DISTINCT utr.user_id) as user_count " +
                   "FROM roles r " +
                   "LEFT JOIN users_tenants_roles utr ON r.id = utr.role_id " +
                   "GROUP BY r.id, r.name " +
                   "ORDER BY user_count DESC",
           nativeQuery = true)
    List<Object[]> getRoleUsageStats();
    
    /**
     * Conta usuários por role (usa índice na pivot table)
     */
    @Query(value = "SELECT COUNT(DISTINCT utr.user_id) " +
                   "FROM users_tenants_roles utr " +
                   "WHERE utr.role_id = :roleId",
           nativeQuery = true)
    long countUsersByRole(@Param("roleId") UUID roleId);
    
    /**
     * Conta permissions por role (usa pivot table roles_permissions)
     */
    @Query(value = "SELECT COUNT(*) " +
                   "FROM roles_permissions rp " +
                   "WHERE rp.role_id = :roleId",
           nativeQuery = true)
    long countPermissionsByRole(@Param("roleId") UUID roleId);

    /**
     * Conta roles por tenant (usa índice FK)
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    // ========================================
    // QUERIES VIA PIVOT TABLE (roles_permissions)
    // ========================================
    
    /**
     * Busca permissions de um role via tabela pivot
     * Query nativa otimizada com JOIN
     */
       @Query(value = "SELECT p.id, p.action, p.resource, rp.policy_id " +
                               "FROM permissions p " +
                               "INNER JOIN roles_permissions rp ON p.id = rp.permission_id " +
                               "WHERE rp.role_id = :roleId " +
                               "ORDER BY p.resource, p.action",
           nativeQuery = true)
    List<Object[]> getRolePermissionsDetails(@Param("roleId") UUID roleId);
    
    /**
     * Verifica se role tem permission específica (usa pivot + índice)
     */
    @Query(value = "SELECT EXISTS(" +
                   "SELECT 1 FROM roles_permissions rp " +
                   "INNER JOIN permissions p ON rp.permission_id = p.id " +
                   "WHERE rp.role_id = :roleId " +
                   "AND p.action = :action " +
                   "AND p.resource = :resource)",
           nativeQuery = true)
    boolean roleHasPermission(@Param("roleId") UUID roleId,
                             @Param("action") String action,
                             @Param("resource") String resource);

    /**
     * Busca usuários de um role via pivot table
     */
    @Query(value = "SELECT DISTINCT u.id, u.name, u.email, u.is_active " +
                   "FROM users u " +
                   "INNER JOIN users_tenants_roles utr ON u.id = utr.user_id " +
                   "WHERE utr.role_id = :roleId " +
                   "ORDER BY u.name",
           nativeQuery = true)
    List<Object[]> getRoleUsersDetails(@Param("roleId") UUID roleId);
    
    /**
     * Busca roles de um tenant com contagem de permissions
     * Query otimizada com LEFT JOIN na pivot
     */
    @Query(value = "SELECT r.id, r.name, r.description, COUNT(rp.permission_id) as perm_count " +
                   "FROM roles r " +
                   "LEFT JOIN roles_permissions rp ON r.id = rp.role_id " +
                   "WHERE r.tenant_id = :tenantId " +
                   "GROUP BY r.id, r.name, r.description " +
                   "ORDER BY r.name",
           nativeQuery = true)
    List<Object[]> findByTenantIdWithPermissionCount(@Param("tenantId") UUID tenantId);
}

