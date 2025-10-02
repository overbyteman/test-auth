package com.seccreto.service.auth.repository.usage;

import com.seccreto.service.auth.model.usage.DailyUserUsage;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Interface para repositório de métricas de uso.
 * Baseado na tabela daily_user_usage criada na migração V10.
 */
public interface UsageRepository {
    
    // Operações básicas CRUD
    DailyUserUsage save(DailyUserUsage usage);
    List<DailyUserUsage> findByUserId(UUID userId);
    List<DailyUserUsage> findByTenantId(UUID tenantId);
    List<DailyUserUsage> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    List<DailyUserUsage> findByDateRange(LocalDate startDate, LocalDate endDate);
    List<DailyUserUsage> findByUserIdAndDateRange(UUID userId, LocalDate startDate, LocalDate endDate);
    List<DailyUserUsage> findByTenantIdAndDateRange(UUID tenantId, LocalDate startDate, LocalDate endDate);
    
    // Operações de agregação
    long countByUserId(UUID userId);
    long countByTenantId(UUID tenantId);
    long countByDateRange(LocalDate startDate, LocalDate endDate);
    
    // Operações de limpeza
    int deleteByDateRange(LocalDate startDate, LocalDate endDate);
    int deleteByUserId(UUID userId);
    int deleteByTenantId(UUID tenantId);
    void clear();
}
