package com.seccreto.service.auth.repository.users;

import com.seccreto.service.auth.model.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para User
 * 
 * Características de implementação sênior:
 * - JPA Repository com queries derivadas automáticas
 * - Queries customizadas com @Query para funcionalidades avançadas
 * - Suporte a paginação e ordenação
 * - Integração com funções PostgreSQL customizadas
 * - Zero código específico de banco (H2/PostgreSQL)
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ========================================
    // QUERIES DERIVADAS AUTOMÁTICAS (JPA)
    // ========================================
    
    Optional<User> findByEmail(String email);
    
    List<User> findByNameContainingIgnoreCase(String name);
    
    List<User> findByIsActive(Boolean isActive);
    
    List<User> findByEmailVerificationTokenIsNotNull();
    
    List<User> findByEmailVerifiedAtIsNull();
    
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    boolean existsByEmail(String email);
    
    long countByIsActive(Boolean isActive);

    // ========================================
    // QUERIES CUSTOMIZADAS COM @Query
    // ========================================
    
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> search(@Param("query") String query);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> search(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.emailVerifiedAt IS NOT NULL")
    List<User> findActiveVerifiedUsers();
    
    @Query("SELECT u FROM User u WHERE u.createdAt >= :since")
    List<User> findRecentUsers(@Param("since") LocalDateTime since);

    // ========================================
    // FUNÇÕES POSTGRESQL CUSTOMIZADAS (NATIVAS)
    // ========================================
    
    /**
     * Usa a função get_user_permissions_in_tenant das migrações V9
     */
    @Query(value = "SELECT p.id, p.action, p.resource " +
                   "FROM get_user_permissions_in_tenant(:userId, :tenantId) p", 
           nativeQuery = true)
    List<Object[]> getUserPermissionsInTenant(@Param("userId") UUID userId, 
                                             @Param("tenantId") UUID tenantId);
    
    /**
     * Usa a função user_has_permission_in_tenant das migrações V9
     */
    @Query(value = "SELECT user_has_permission_in_tenant(:userId, :tenantId, :action, :resource)", 
           nativeQuery = true)
    Boolean userHasPermissionInTenant(@Param("userId") UUID userId, 
                                     @Param("tenantId") UUID tenantId,
                                     @Param("action") String action, 
                                     @Param("resource") String resource);
    
    /**
     * Usa a função get_user_tenants_with_roles das migrações V9
     */
    @Query(value = "SELECT t.tenant_id, t.tenant_name, t.role_id, t.role_name, t.role_description " +
                   "FROM get_user_tenants_with_roles(:userId) t", 
           nativeQuery = true)
    List<Object[]> getUserTenantsWithRoles(@Param("userId") UUID userId);
    
    /**
     * Usa a função record_user_login das migrações V10
     */
    @Query(value = "SELECT record_user_login(:userId, :tenantId)", nativeQuery = true)
    void recordUserLogin(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);
    
    /**
     * Usa a função record_user_action das migrações V10
     */
    @Query(value = "SELECT record_user_action(:userId, :tenantId)", nativeQuery = true)
    void recordUserAction(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    // ========================================
    // QUERIES DE MÉTRICAS E RELATÓRIOS
    // ========================================
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countUsersCreatedSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT u.isActive, COUNT(u) FROM User u GROUP BY u.isActive")
    List<Object[]> getUserStatusDistribution();
    
    @Query("SELECT DATE(u.createdAt), COUNT(u) FROM User u " +
           "WHERE u.createdAt >= :since GROUP BY DATE(u.createdAt) ORDER BY DATE(u.createdAt)")
    List<Object[]> getUserCreationStats(@Param("since") LocalDateTime since);

    // ========================================
    // MÉTODOS PARA COMPATIBILIDADE COM SERVICES
    // ========================================
    
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByName(@Param("name") String name);
    
    @Query(value = "SELECT u.* FROM users u WHERE u.id IN " +
                   "(SELECT utr.user_id FROM users_tenants_roles utr WHERE utr.tenant_id = :tenantId)",
           nativeQuery = true)
    List<User> findUsersByTenant(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.createdAt DESC")
    List<User> findTopActiveUsers(Pageable pageable);
    
    default List<User> findTopActiveUsers(int limit) {
        return findTopActiveUsers(Pageable.ofSize(limit));
    }
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = false")
    long countSuspendedUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startOfDay AND u.createdAt < :endOfDay")
    long countUsersCreatedToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    default long countUsersCreatedToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return countUsersCreatedToday(startOfDay, endOfDay);
    }
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :weekStart")
    long countUsersCreatedThisWeek(@Param("weekStart") LocalDateTime weekStart);
    
    default long countUsersCreatedThisWeek() {
        return countUsersCreatedThisWeek(LocalDateTime.now().minusDays(7));
    }
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :monthStart")
    long countUsersCreatedThisMonth(@Param("monthStart") LocalDateTime monthStart);
    
    default long countUsersCreatedThisMonth() {
        return countUsersCreatedThisMonth(LocalDateTime.now().minusDays(30));
    }
    
    @Query(value = "SELECT COUNT(u.*) FROM users u WHERE u.id IN " +
                   "(SELECT utr.user_id FROM users_tenants_roles utr WHERE utr.tenant_id = :tenantId)",
           nativeQuery = true)
    long countUsersByTenant(@Param("tenantId") UUID tenantId);
    
    @Query(value = "SELECT COUNT(u.*) FROM users u WHERE u.is_active = true AND u.id IN " +
                   "(SELECT utr.user_id FROM users_tenants_roles utr WHERE utr.tenant_id = :tenantId)",
           nativeQuery = true)
    long countActiveUsersByTenant(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end")
    long countUsersInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    default long countUsersInPeriod(String startDate, String endDate) {
        return countUsersInPeriod(LocalDateTime.parse(startDate), LocalDateTime.parse(endDate));
    }
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true AND u.createdAt BETWEEN :start AND :end")
    long countActiveUsersInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    default long countActiveUsersInPeriod(String startDate, String endDate) {
        return countActiveUsersInPeriod(LocalDateTime.parse(startDate), LocalDateTime.parse(endDate));
    }
}