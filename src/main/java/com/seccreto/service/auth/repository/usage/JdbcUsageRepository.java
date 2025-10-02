package com.seccreto.service.auth.repository.usage;

import com.seccreto.service.auth.model.usage.DailyUserUsage;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementação de UsageRepository usando JDBC + PostgreSQL.
 * Baseado na tabela daily_user_usage criada na migração V10.
 *
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a agregações de uso
 */
@Repository
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class JdbcUsageRepository implements UsageRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public JdbcUsageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * RowMapper otimizado para DailyUserUsage
     */
    private static final RowMapper<DailyUserUsage> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        DailyUserUsage usage = new DailyUserUsage();
        usage.setUsageDate(rs.getObject("usage_date", LocalDate.class));
        usage.setUserId(rs.getObject("user_id", UUID.class));
        usage.setTenantId(rs.getObject("tenant_id", UUID.class));
        usage.setLogins(rs.getInt("logins"));
        usage.setActions(rs.getInt("actions"));
        usage.setLastActionAt(rs.getObject("last_action_at", LocalDateTime.class));
        usage.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        usage.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return usage;
    };

    @Override
    @Transactional
    public DailyUserUsage save(DailyUserUsage usage) {
        try {
            String sql = """
                INSERT INTO daily_user_usage (usage_date, user_id, tenant_id, logins, actions, last_action_at)
                VALUES (:usageDate, :userId, :tenantId, :logins, :actions, :lastActionAt)
                ON CONFLICT (usage_date, user_id, tenant_id) DO UPDATE
                SET logins = daily_user_usage.logins + :logins,
                    actions = daily_user_usage.actions + :actions,
                    last_action_at = GREATEST(daily_user_usage.last_action_at, :lastActionAt),
                    updated_at = NOW()
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("usageDate", usage.getUsageDate())
                    .addValue("userId", usage.getUserId())
                    .addValue("tenantId", usage.getTenantId())
                    .addValue("logins", usage.getLogins())
                    .addValue("actions", usage.getActions())
                    .addValue("lastActionAt", usage.getLastActionAt());

            namedParameterJdbcTemplate.update(sql, params);
            return usage;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar métrica de uso: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DailyUserUsage> findByUserId(UUID userId) {
        try {
            String sql = "SELECT * FROM daily_user_usage WHERE user_id = :userId ORDER BY usage_date DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar métricas por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DailyUserUsage> findByTenantId(UUID tenantId) {
        try {
            String sql = "SELECT * FROM daily_user_usage WHERE tenant_id = :tenantId ORDER BY usage_date DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar métricas por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DailyUserUsage> findByUserIdAndTenantId(UUID userId, UUID tenantId) {
        try {
            String sql = "SELECT * FROM daily_user_usage WHERE user_id = :userId AND tenant_id = :tenantId ORDER BY usage_date DESC";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar métricas por usuário e tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DailyUserUsage> findByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            String sql = "SELECT * FROM daily_user_usage WHERE usage_date BETWEEN :startDate AND :endDate ORDER BY usage_date DESC";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar métricas por período: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DailyUserUsage> findByUserIdAndDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        try {
            String sql = "SELECT * FROM daily_user_usage WHERE user_id = :userId AND usage_date BETWEEN :startDate AND :endDate ORDER BY usage_date DESC";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar métricas do usuário por período: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DailyUserUsage> findByTenantIdAndDateRange(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        try {
            String sql = "SELECT * FROM daily_user_usage WHERE tenant_id = :tenantId AND usage_date BETWEEN :startDate AND :endDate ORDER BY usage_date DESC";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar métricas do tenant por período: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByUserId(UUID userId) {
        try {
            String sql = "SELECT COUNT(1) FROM daily_user_usage WHERE user_id = :userId";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar métricas por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByTenantId(UUID tenantId) {
        try {
            String sql = "SELECT COUNT(1) FROM daily_user_usage WHERE tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar métricas por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            String sql = "SELECT COUNT(1) FROM daily_user_usage WHERE usage_date BETWEEN :startDate AND :endDate";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar métricas por período: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int deleteByDateRange(LocalDate startDate, LocalDate endDate) {
        try {
            String sql = "DELETE FROM daily_user_usage WHERE usage_date BETWEEN :startDate AND :endDate";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate);
            return namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar métricas por período: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int deleteByUserId(UUID userId) {
        try {
            String sql = "DELETE FROM daily_user_usage WHERE user_id = :userId";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            return namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar métricas por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int deleteByTenantId(UUID tenantId) {
        try {
            String sql = "DELETE FROM daily_user_usage WHERE tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            return namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar métricas por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void clear() {
        try {
            String sql = "DELETE FROM daily_user_usage";
            jdbcTemplate.update(sql);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao limpar métricas: " + e.getMessage(), e);
        }
    }
}
