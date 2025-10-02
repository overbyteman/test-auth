package com.seccreto.service.auth.repository.usage;

import com.seccreto.service.auth.model.usage.DailyUserUsage;
import com.seccreto.service.auth.model.usage.DailyUserUsageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyUserUsageRepository extends JpaRepository<DailyUserUsage, DailyUserUsageId> {

    List<DailyUserUsage> findByUserId(UUID userId);

    List<DailyUserUsage> findByTenantId(UUID tenantId);

    List<DailyUserUsage> findByUsageDate(LocalDate usageDate);

    List<DailyUserUsage> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    List<DailyUserUsage> findByUserIdAndUsageDate(UUID userId, LocalDate usageDate);

    List<DailyUserUsage> findByTenantIdAndUsageDate(UUID tenantId, LocalDate usageDate);

    Optional<DailyUserUsage> findByUsageDateAndUserIdAndTenantId(LocalDate usageDate, UUID userId, UUID tenantId);

    // ========================================
    // QUERIES CUSTOMIZADAS PARA ESTATÍSTICAS
    // ========================================

    @Query("SELECT SUM(d.logins) FROM DailyUserUsage d WHERE d.userId = :userId AND d.usageDate >= :since")
    Long getTotalLoginsByUser(@Param("userId") UUID userId, @Param("since") LocalDate since);

    @Query("SELECT SUM(d.actions) FROM DailyUserUsage d WHERE d.userId = :userId AND d.usageDate >= :since")
    Long getTotalActionsByUser(@Param("userId") UUID userId, @Param("since") LocalDate since);

    @Query("SELECT SUM(d.logins) FROM DailyUserUsage d WHERE d.tenantId = :tenantId AND d.usageDate >= :since")
    Long getTotalLoginsByTenant(@Param("tenantId") UUID tenantId, @Param("since") LocalDate since);

    @Query("SELECT SUM(d.actions) FROM DailyUserUsage d WHERE d.tenantId = :tenantId AND d.usageDate >= :since")
    Long getTotalActionsByTenant(@Param("tenantId") UUID tenantId, @Param("since") LocalDate since);

    @Query("SELECT d.usageDate, SUM(d.logins), SUM(d.actions) FROM DailyUserUsage d " +
           "WHERE d.usageDate >= :since GROUP BY d.usageDate ORDER BY d.usageDate")
    List<Object[]> getDailyUsageStats(@Param("since") LocalDate since);

    @Query("SELECT d.userId, SUM(d.logins), SUM(d.actions) FROM DailyUserUsage d " +
           "WHERE d.usageDate >= :since GROUP BY d.userId ORDER BY SUM(d.logins) DESC")
    List<Object[]> getUserUsageStats(@Param("since") LocalDate since);

    @Query("SELECT d.tenantId, SUM(d.logins), SUM(d.actions) FROM DailyUserUsage d " +
           "WHERE d.usageDate >= :since GROUP BY d.tenantId ORDER BY SUM(d.logins) DESC")
    List<Object[]> getTenantUsageStats(@Param("since") LocalDate since);

    // ========================================
    // MÉTODOS PARA COMPATIBILIDADE COM SERVICES
    // ========================================

    @Query("SELECT COUNT(DISTINCT d.userId) FROM DailyUserUsage d WHERE d.usageDate = :date")
    long countActiveUsersOnDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(DISTINCT d.userId) FROM DailyUserUsage d WHERE d.usageDate >= :since")
    long countActiveUsersSince(@Param("since") LocalDate since);

    @Query("SELECT COUNT(DISTINCT d.userId) FROM DailyUserUsage d WHERE d.tenantId = :tenantId AND d.usageDate >= :since")
    long countActiveUsersByTenantSince(@Param("tenantId") UUID tenantId, @Param("since") LocalDate since);

    @Query("SELECT SUM(d.logins) FROM DailyUserUsage d WHERE d.usageDate = :date")
    Long getTotalLoginsOnDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(d.actions) FROM DailyUserUsage d WHERE d.usageDate = :date")
    Long getTotalActionsOnDate(@Param("date") LocalDate date);

    @Query("SELECT d FROM DailyUserUsage d WHERE d.usageDate BETWEEN :startDate AND :endDate ORDER BY d.usageDate")
    List<DailyUserUsage> findByUsageDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ========================================
    // MÉTODOS DE USAGE (IMPLEMENTAÇÃO JAVA)
    // ========================================
    // Funções PostgreSQL removidas - implementação será feita nos services usando JPA
}
