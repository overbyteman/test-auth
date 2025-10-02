package com.seccreto.service.auth.repository.audit;

import com.seccreto.service.auth.model.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para logs de auditoria
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Buscar por usuário
    List<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId);
    
    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    // Buscar por ação
    List<AuditLog> findByActionOrderByTimestampDesc(String action);
    
    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    // Buscar por período
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    // Buscar por usuário e período
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<AuditLog> findByUserIdAndTimestampBetween(@Param("userId") UUID userId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    // Buscar falhas de segurança
    List<AuditLog> findBySuccessFalseOrderByTimestampDesc();
    
    Page<AuditLog> findBySuccessFalseOrderByTimestampDesc(Pageable pageable);

    // Buscar por IP
    List<AuditLog> findByIpAddressOrderByTimestampDesc(String ipAddress);

    // Estatísticas
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action AND a.timestamp >= :since")
    long countByActionSince(@Param("action") String action, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId AND a.timestamp >= :since")
    long countByUserIdSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.success = false AND a.timestamp >= :since")
    long countFailuresSince(@Param("since") LocalDateTime since);

    // Limpeza de logs antigos
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
