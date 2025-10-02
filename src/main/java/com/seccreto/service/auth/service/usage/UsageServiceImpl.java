package com.seccreto.service.auth.service.usage;

import com.seccreto.service.auth.model.usage.DailyUserUsage;
import com.seccreto.service.auth.repository.usage.UsageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final UsageRepository usageRepository;

    public UsageServiceImpl(JdbcTemplate jdbcTemplate, UsageRepository usageRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.usageRepository = usageRepository;
    }

    @Override
    @Transactional
    public void recordUserLogin(UUID userId, UUID tenantId, LocalDate usageDate) {
        try {
            String sql = "SELECT record_user_login(:userId, :tenantId, :usageDate)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId)
                    .addValue("usageDate", usageDate);
            
            namedParameterJdbcTemplate.queryForObject(sql, params, Void.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar login do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void recordUserAction(UUID userId, UUID tenantId, LocalDate usageDate, LocalDateTime actionAt) {
        try {
            String sql = "SELECT record_user_action(:userId, :tenantId, :usageDate, :actionAt)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId)
                    .addValue("usageDate", usageDate)
                    .addValue("actionAt", actionAt);
            
            namedParameterJdbcTemplate.queryForObject(sql, params, Void.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao registrar ação do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DailyUserUsage> getUserDailyUsage(UUID userId, LocalDate startDate, LocalDate endDate, UUID tenantId) {
        try {
            String sql = "SELECT * FROM get_user_daily_usage(:userId, :startDate, :endDate, :tenantId)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate)
                    .addValue("tenantId", tenantId);
            
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
                DailyUserUsage usage = new DailyUserUsage();
                usage.setUsageDate(rs.getObject("usage_date", LocalDate.class));
                usage.setUserId(rs.getObject("tenant_id", UUID.class));
                usage.setTenantId(rs.getObject("tenant_id", UUID.class));
                usage.setLogins(rs.getInt("logins"));
                usage.setActions(rs.getInt("actions"));
                usage.setLastActionAt(rs.getObject("last_action_at", LocalDateTime.class));
                return usage;
            });
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter métricas de uso do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getTopActiveUsers(LocalDate startDate, LocalDate endDate, UUID tenantId, int limit) {
        try {
            String sql = "SELECT * FROM get_top_active_users(:startDate, :endDate, :tenantId, :limit)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate)
                    .addValue("tenantId", tenantId)
                    .addValue("limit", limit);
            
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
                Map<String, Object> user = Map.of(
                    "userId", rs.getObject("user_id", UUID.class),
                    "totalActions", rs.getLong("total_actions"),
                    "totalLogins", rs.getLong("total_logins"),
                    "lastActionAt", rs.getObject("last_action_at", LocalDateTime.class)
                );
                return user;
            });
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter usuários mais ativos: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int cleanupOldUsage(int keepDays) {
        try {
            String sql = "SELECT cleanup_old_usage(:keepDays)";
            MapSqlParameterSource params = new MapSqlParameterSource("keepDays", keepDays);
            
            Integer deletedCount = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return deletedCount != null ? deletedCount : 0;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao limpar dados antigos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getUserPermissionsInTenant(UUID userId, UUID tenantId) {
        try {
            String sql = "SELECT * FROM get_user_permissions_in_tenant(:userId, :tenantId)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
                Map<String, Object> permission = Map.of(
                    "permissionId", rs.getObject("permission_id", UUID.class),
                    "action", rs.getString("action"),
                    "resource", rs.getString("resource")
                );
                return permission;
            });
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter permissões do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean userHasPermissionInTenant(UUID userId, UUID tenantId, String action, String resource) {
        try {
            String sql = "SELECT user_has_permission_in_tenant(:userId, :tenantId, :action, :resource)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId)
                    .addValue("action", action)
                    .addValue("resource", resource);
            
            Boolean hasPermission = namedParameterJdbcTemplate.queryForObject(sql, params, Boolean.class);
            return hasPermission != null && hasPermission;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar permissão do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getUserTenantsWithRoles(UUID userId) {
        try {
            String sql = "SELECT * FROM get_user_tenants_with_roles(:userId)";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
                Map<String, Object> tenantRole = Map.of(
                    "tenantId", rs.getObject("tenant_id", UUID.class),
                    "tenantName", rs.getString("tenant_name"),
                    "roleId", rs.getObject("role_id", UUID.class),
                    "roleName", rs.getString("role_name"),
                    "roleDescription", rs.getString("role_description")
                );
                return tenantRole;
            });
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter tenants do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> evaluateAbacPolicies(String action, String resource, String context) {
        try {
            String sql = "SELECT * FROM evaluate_abac_policies(:action, :resource, :context)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", action)
                    .addValue("resource", resource)
                    .addValue("context", context);
            
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
                Map<String, Object> policy = Map.of(
                    "policyId", rs.getObject("policy_id", UUID.class),
                    "policyName", rs.getString("policy_name"),
                    "effect", rs.getString("effect"),
                    "conditionsMatch", rs.getBoolean("conditions_match")
                );
                return policy;
            });
        } catch (Exception e) {
            throw new RuntimeException("Erro ao avaliar políticas ABAC: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int cleanupExpiredSessions() {
        try {
            String sql = "SELECT cleanup_expired_sessions()";
            Integer deletedCount = jdbcTemplate.queryForObject(sql, Integer.class);
            return deletedCount != null ? deletedCount : 0;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao limpar sessões expiradas: " + e.getMessage(), e);
        }
    }
}
