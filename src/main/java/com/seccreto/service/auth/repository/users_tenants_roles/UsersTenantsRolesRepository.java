package com.seccreto.service.auth.repository.users_tenants_roles;

import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;
import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRolesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para UsersTenantsRoles
 */
@Repository
public interface UsersTenantsRolesRepository extends JpaRepository<UsersTenantsRoles, UsersTenantsRolesId> {

    // ========================================
    // QUERIES DERIVADAS AUTOMÁTICAS (JPA)
    // ========================================
    
    List<UsersTenantsRoles> findByUserId(UUID userId);
    
    List<UsersTenantsRoles> findByTenantId(UUID tenantId);
    
    List<UsersTenantsRoles> findByRoleId(UUID roleId);
    
    List<UsersTenantsRoles> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    
    List<UsersTenantsRoles> findByUserIdAndRoleId(UUID userId, UUID roleId);
    
    List<UsersTenantsRoles> findByTenantIdAndRoleId(UUID tenantId, UUID roleId);
    
    boolean existsByUserIdAndTenantIdAndRoleId(UUID userId, UUID tenantId, UUID roleId);
    
    // Métodos adicionais para compatibilidade com services
    Optional<UsersTenantsRoles> findByUserIdAndTenantIdAndRoleId(UUID userId, UUID tenantId, UUID roleId);
    
    void deleteByUserIdAndTenantIdAndRoleId(UUID userId, UUID tenantId, UUID roleId);
    
    void deleteByUserIdAndTenantId(UUID userId, UUID tenantId);
    
    void deleteByTenantIdAndRoleId(UUID tenantId, UUID roleId);
    
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
    
    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
    
    boolean existsByTenantIdAndRoleId(UUID tenantId, UUID roleId);
    
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
    
    long countByUserIdAndTenantId(UUID userId, UUID tenantId);
    
    long countByTenantIdAndRoleId(UUID tenantId, UUID roleId);
    
    long countByUserIdAndRoleId(UUID userId, UUID roleId);

    // ========================================
    // QUERIES CUSTOMIZADAS COM @Query
    // ========================================
    
    @Query(value = "SELECT COUNT(*) FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId", nativeQuery = true)
    long countRolesByUser(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
    
    @Query(value = "SELECT COUNT(*) FROM users_tenants_roles", nativeQuery = true)
    long countAssociations();
    
    @Query(value = "SELECT DISTINCT role_id FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId", nativeQuery = true)
    List<UUID> findRoleIdsByUser(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
    
    @Query(value = "SELECT DISTINCT user_id FROM users_tenants_roles WHERE tenant_id = :tenantId AND role_id = :roleId", nativeQuery = true)
    List<UUID> findUserIdsByTenantAndRole(@Param("tenantId") UUID tenantId, @Param("roleId") UUID roleId);

    // ========================================
    // QUERIES PARA NOMES (JOIN COM OUTRAS TABELAS)
    // ========================================
    
    @Query(value = "SELECT r.name FROM users_tenants_roles utr JOIN roles r ON utr.role_id = r.id " +
                   "WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId", nativeQuery = true)
    List<String> findRoleNamesByUser(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
    
    @Query(value = "SELECT DISTINCT p.action FROM users_tenants_roles utr " +
                   "JOIN roles_permissions rp ON utr.role_id = rp.role_id " +
                   "JOIN permissions p ON rp.permission_id = p.id " +
                   "WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId", nativeQuery = true)
    List<String> findPermissionNamesByUser(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
    
    @Query(value = "SELECT COUNT(DISTINCT utr.role_id) FROM users_tenants_roles utr " +
                   "WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId", nativeQuery = true)
    long countRolesByUserInTenant(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
    
    @Query(value = "SELECT COUNT(DISTINCT p.id) FROM users_tenants_roles utr " +
                   "JOIN roles_permissions rp ON utr.role_id = rp.role_id " +
                   "JOIN permissions p ON rp.permission_id = p.id " +
                   "WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId", nativeQuery = true)
    long countPermissionsByUser(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    // ========================================
    // QUERIES PARA SUBSTITUIR JDBC
    // ========================================
    
    @Query(value = "SELECT COUNT(1) FROM users_tenants_roles utr " +
                   "JOIN roles r ON utr.role_id = r.id " +
                   "WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId AND r.name = :roleName", 
           nativeQuery = true)
    long countUserTenantRoleByName(@Param("userId") UUID userId, 
                                  @Param("tenantId") UUID tenantId, 
                                  @Param("roleName") String roleName);
    
    @Query(value = "SELECT r.id, r.name, r.description FROM roles r " +
                   "JOIN users_tenants_roles utr ON r.id = utr.role_id " +
                   "WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId " +
                   "ORDER BY r.name", nativeQuery = true)
    List<Object[]> getUserRolesInTenantDetails(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
    
    @Query(value = "SELECT u.id, u.name, u.email, u.is_active FROM users u " +
                   "JOIN users_tenants_roles utr ON u.id = utr.user_id " +
                   "WHERE utr.tenant_id = :tenantId AND utr.role_id = :roleId " +
                   "ORDER BY u.name", nativeQuery = true)
    List<Object[]> getUsersInTenantWithRoleDetails(@Param("tenantId") UUID tenantId, @Param("roleId") UUID roleId);
    
    @Query(value = "SELECT t.id, t.name, t.config FROM tenants t " +
                   "JOIN users_tenants_roles utr ON t.id = utr.tenant_id " +
                   "WHERE utr.user_id = :userId AND utr.role_id = :roleId " +
                   "ORDER BY t.name", nativeQuery = true)
    List<Object[]> getTenantsForUserWithRoleDetails(@Param("userId") UUID userId, @Param("roleId") UUID roleId);
    
    @Query(value = "SELECT DISTINCT r.name FROM users_tenants_roles utr " +
                   "JOIN roles r ON utr.role_id = r.id " +
                   "WHERE utr.user_id = :userId", nativeQuery = true)
    List<String> getAllUserRoleNames(@Param("userId") UUID userId);
    
    @Query(value = "SELECT DISTINCT p.action FROM users_tenants_roles utr " +
                   "JOIN roles_permissions rp ON utr.role_id = rp.role_id " +
                   "JOIN permissions p ON rp.permission_id = p.id " +
                   "WHERE utr.user_id = :userId", nativeQuery = true)
    List<String> getAllUserPermissionNames(@Param("userId") UUID userId);
    
    @Query(value = "SELECT COUNT(DISTINCT utr.role_id) FROM users_tenants_roles utr " +
                   "WHERE utr.user_id = :userId", nativeQuery = true)
    long countAllUserRoles(@Param("userId") UUID userId);
    
    @Query(value = "SELECT COUNT(DISTINCT p.id) FROM users_tenants_roles utr " +
                   "JOIN roles_permissions rp ON utr.role_id = rp.role_id " +
                   "JOIN permissions p ON rp.permission_id = p.id " +
                   "WHERE utr.user_id = :userId", nativeQuery = true)
    long countAllUserPermissions(@Param("userId") UUID userId);

    // ========================================
    // MÉTODOS DE ASSOCIAÇÃO
    // ========================================
    
    default UsersTenantsRoles createAssociation(UUID userId, UUID tenantId, UUID roleId) {
        UsersTenantsRoles association = UsersTenantsRoles.createNew(userId, tenantId, roleId);
        return save(association);
    }
    
    default void removeAssociation(UUID userId, UUID tenantId, UUID roleId) {
        UsersTenantsRolesId id = new UsersTenantsRolesId(userId, tenantId, roleId);
        deleteById(id);
    }
}