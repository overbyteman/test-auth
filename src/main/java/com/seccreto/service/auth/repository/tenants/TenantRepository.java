package com.seccreto.service.auth.repository.tenants;

import com.seccreto.service.auth.model.tenants.Tenant;
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
 * Repository JPA para Tenant
 * 
 * Características de implementação sênior:
 * - JPA Repository com queries derivadas automáticas
 * - Suporte a JSON com Hibernate
 * - Queries customizadas para multi-tenancy
 * - Zero código específico de banco (H2/PostgreSQL)
 * - Paginação e ordenação automáticas
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    // ========================================
    // QUERIES DERIVADAS AUTOMÁTICAS (JPA)
    // ========================================
    
    Optional<Tenant> findByName(String name);
    
    List<Tenant> findByNameContainingIgnoreCase(String name);
    
    List<Tenant> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    boolean existsByName(String name);

    // ========================================
    // QUERIES CUSTOMIZADAS COM @Query
    // ========================================
    
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Tenant> search(@Param("query") String query);
    
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Tenant> search(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT t FROM Tenant t WHERE t.createdAt >= :since")
    List<Tenant> findRecentTenants(@Param("since") LocalDateTime since);

    // ========================================
    // QUERIES JSON (PostgreSQL/H2 compatível)
    // ========================================
    
    /**
     * Busca tenants por configuração JSON
     * Funciona tanto em PostgreSQL (JSONB) quanto H2 (JSON)
     */
    @Query(value = "SELECT * FROM tenants WHERE JSON_EXTRACT(config, '$.active') = true", 
           nativeQuery = true)
    List<Tenant> findActiveTenants();
    
    @Query(value = "SELECT * FROM tenants WHERE JSON_EXTRACT(config, '$.type') = :type", 
           nativeQuery = true)
    List<Tenant> findByConfigType(@Param("type") String type);

    // ========================================
    // QUERIES DE MÉTRICAS E RELATÓRIOS
    // ========================================
    
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.createdAt >= :since")
    long countTenantsCreatedSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT DATE(t.createdAt), COUNT(t) FROM Tenant t " +
           "WHERE t.createdAt >= :since GROUP BY DATE(t.createdAt) ORDER BY DATE(t.createdAt)")
    List<Object[]> getTenantCreationStats(@Param("since") LocalDateTime since);
    
    /**
     * Estatísticas de uso por tenant usando as funções das migrações V10
     */
    @Query(value = "SELECT t.id, t.name, " +
                   "COALESCE(SUM(d.logins), 0) as total_logins, " +
                   "COALESCE(SUM(d.actions), 0) as total_actions " +
                   "FROM tenants t " +
                   "LEFT JOIN daily_user_usage d ON t.id = d.tenant_id " +
                   "WHERE d.usage_date >= :since OR d.usage_date IS NULL " +
                   "GROUP BY t.id, t.name " +
                   "ORDER BY total_logins DESC", 
           nativeQuery = true)
    List<Object[]> getTenantUsageStats(@Param("since") LocalDateTime since);

    // ========================================
    // MÉTODOS PARA COMPATIBILIDADE COM SERVICES
    // ========================================
    
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) = LOWER(:name)")
    Optional<Tenant> findByNameExact(@Param("name") String name);
    
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.createdAt >= :startOfDay AND t.createdAt < :endOfDay")
    long countCreatedToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    default long countCreatedToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return countCreatedToday(startOfDay, endOfDay);
    }
    
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.createdAt >= :weekStart")
    long countCreatedThisWeek(@Param("weekStart") LocalDateTime weekStart);
    
    default long countCreatedThisWeek() {
        return countCreatedThisWeek(LocalDateTime.now().minusDays(7));
    }
    
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.createdAt >= :monthStart")
    long countCreatedThisMonth(@Param("monthStart") LocalDateTime monthStart);
    
    default long countCreatedThisMonth() {
        return countCreatedThisMonth(LocalDateTime.now().minusDays(30));
    }
    
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.createdAt BETWEEN :start AND :end")
    long countInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    default long countInPeriod(String startDate, String endDate) {
        return countInPeriod(LocalDateTime.parse(startDate), LocalDateTime.parse(endDate));
    }

    // ========================================
    // QUERIES PARA SUBSTITUIR JDBC
    // ========================================
    
    @Query(value = "SELECT u.id, u.name, u.email, u.is_active, u.created_at FROM users u " +
                   "JOIN users_tenants_roles utr ON u.id = utr.user_id " +
                   "WHERE utr.tenant_id = :tenantId ORDER BY u.name", nativeQuery = true)
    List<Object[]> getTenantUsersDetails(@Param("tenantId") UUID tenantId);
    
    @Query(value = "SELECT COUNT(DISTINCT u.id) FROM users u " +
                   "JOIN users_tenants_roles utr ON u.id = utr.user_id " +
                   "WHERE utr.tenant_id = :tenantId", nativeQuery = true)
    long countTenantUsers(@Param("tenantId") UUID tenantId);
}