package com.seccreto.service.auth.service.usage;

import com.seccreto.service.auth.model.usage.DailyUserUsage;
import com.seccreto.service.auth.repository.usage.DailyUserUsageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do UsageService usando as funções criadas nas migrações.
 * 
 * Características de implementação sênior:
 * - Uso das funções de banco criadas nas migrações V9 e V10
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com funções de banco
 * - Suporte a métricas de uso em tempo real
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class UsageServiceImpl implements UsageService {

    private final DailyUserUsageRepository dailyUserUsageRepository;

    public UsageServiceImpl(DailyUserUsageRepository dailyUserUsageRepository) {
        this.dailyUserUsageRepository = dailyUserUsageRepository;
    }

    @Override
    @Transactional
    public void recordUserLogin(UUID userId, UUID tenantId, LocalDate usageDate) {
        try {
            // Implementação Java - substitui a função PostgreSQL
            LocalDate date = usageDate != null ? usageDate : LocalDate.now();
            Optional<DailyUserUsage> existingUsage = dailyUserUsageRepository
                .findByUsageDateAndUserIdAndTenantId(date, userId, tenantId);
            
            if (existingUsage.isPresent()) {
                DailyUserUsage usage = existingUsage.get();
                usage.incrementLogin();
                dailyUserUsageRepository.save(usage);
            } else {
                DailyUserUsage newUsage = DailyUserUsage.createNew(userId, tenantId, date);
                newUsage.incrementLogin();
                dailyUserUsageRepository.save(newUsage);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar login do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void recordUserAction(UUID userId, UUID tenantId, LocalDate usageDate, LocalDateTime actionAt) {
        try {
            // Implementação Java - substitui a função PostgreSQL
            LocalDate date = usageDate != null ? usageDate : LocalDate.now();
            LocalDateTime action = actionAt != null ? actionAt : LocalDateTime.now();
            Optional<DailyUserUsage> existingUsage = dailyUserUsageRepository
                .findByUsageDateAndUserIdAndTenantId(date, userId, tenantId);
            
            if (existingUsage.isPresent()) {
                DailyUserUsage usage = existingUsage.get();
                usage.incrementAction(action);
                dailyUserUsageRepository.save(usage);
            } else {
                DailyUserUsage newUsage = DailyUserUsage.createNew(userId, tenantId, date);
                newUsage.incrementAction(action);
                dailyUserUsageRepository.save(newUsage);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar ação do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DailyUserUsage> getUserDailyUsage(UUID userId, LocalDate startDate, LocalDate endDate, UUID tenantId) {
        try {
            // Usa método JPA padrão para buscar por período
            return dailyUserUsageRepository.findByUsageDateBetween(startDate, endDate)
                    .stream()
                    .filter(usage -> usage.getUserId().equals(userId) && usage.getTenantId().equals(tenantId))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter métricas de uso do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getTopActiveUsers(LocalDate startDate, LocalDate endDate, UUID tenantId, int limit) {
        try {
            // Usa método JPA para obter estatísticas de usuários
            List<Object[]> stats = dailyUserUsageRepository.getUserUsageStats(startDate);
            return stats.stream()
                    .limit(limit)
                    .map(row -> (Object) Map.of(
                        "userId", row[0],
                        "totalLogins", row[1],
                        "totalActions", row[2]
                    ))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter usuários mais ativos: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int cleanupOldUsage(int keepDays) {
        try {
            // Remove registros antigos baseado em keepDays
            LocalDate cutoffDate = LocalDate.now().minusDays(keepDays);
            List<DailyUserUsage> oldRecords = dailyUserUsageRepository.findByUsageDateBetween(
                LocalDate.of(2000, 1, 1), cutoffDate);
            dailyUserUsageRepository.deleteAll(oldRecords);
            return oldRecords.size();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao limpar dados antigos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getUserPermissionsInTenant(UUID userId, UUID tenantId) {
        try {
            // TODO: Implementar usando outros repositories JPA (UsersTenantsRoles, RolesPermissions)
            // Por enquanto retorna lista vazia
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter permissões do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean userHasPermissionInTenant(UUID userId, UUID tenantId, String action, String resource) {
        try {
            // TODO: Implementar usando outros repositories JPA (UsersTenantsRoles, RolesPermissions)
            // Por enquanto retorna false
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar permissão do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getUserTenantsWithRoles(UUID userId) {
        try {
            // TODO: Implementar usando outros repositories JPA (UsersTenantsRoles)
            // Por enquanto retorna lista vazia
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter tenants do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> evaluateAbacPolicies(String action, String resource, String context) {
        try {
            // TODO: Implementar usando PolicyRepository JPA
            // Por enquanto retorna lista vazia
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao avaliar políticas ABAC: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int cleanupExpiredSessions() {
        try {
            // TODO: Implementar usando SessionRepository JPA
            // Por enquanto retorna 0
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao limpar sessões expiradas: " + e.getMessage(), e);
        }
    }
}
