package com.seccreto.service.auth.repository.users_tenants_roles;

import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Implementação de UsersTenantsRolesRepository usando JDBC + PostgreSQL.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a multi-tenancy com relacionamentos complexos
 */
@Repository
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class JdbcUsersTenantsRolesRepository implements UsersTenantsRolesRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public JdbcUsersTenantsRolesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * RowMapper otimizado com tratamento de timezone
     */
    private static final RowMapper<UsersTenantsRoles> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        UsersTenantsRoles usersTenantsRoles = new UsersTenantsRoles();
        usersTenantsRoles.setUserId(rs.getLong("user_id"));
        usersTenantsRoles.setTenantId(rs.getLong("tenant_id"));
        usersTenantsRoles.setRoleId(rs.getLong("role_id"));
        
        // Tratamento seguro de timestamps com timezone
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            usersTenantsRoles.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return usersTenantsRoles;
    };

    @Override
    @Transactional
    public UsersTenantsRoles save(UsersTenantsRoles usersTenantsRoles) {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            usersTenantsRoles.setCreatedAt(now);

            String sql = """
                INSERT INTO users_tenants_roles (user_id, tenant_id, role_id, created_at) 
                VALUES (:userId, :tenantId, :roleId, :createdAt)
                ON CONFLICT (user_id, tenant_id, role_id) DO NOTHING
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", usersTenantsRoles.getUserId())
                    .addValue("tenantId", usersTenantsRoles.getTenantId())
                    .addValue("roleId", usersTenantsRoles.getRoleId())
                    .addValue("createdAt", usersTenantsRoles.getCreatedAt());

            namedParameterJdbcTemplate.update(sql, params);
            return usersTenantsRoles;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar relação user-tenant-role: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<UsersTenantsRoles> findByUserIdAndTenantIdAndRoleId(Long userId, Long tenantId, Long roleId) {
        try {
            String sql = "SELECT * FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            
            List<UsersTenantsRoles> relations = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return relations.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar relação user-tenant-role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UsersTenantsRoles> findByUserId(Long userId) {
        try {
            String sql = "SELECT * FROM users_tenants_roles WHERE user_id = :userId ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar relações por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UsersTenantsRoles> findByTenantId(Long tenantId) {
        try {
            String sql = "SELECT * FROM users_tenants_roles WHERE tenant_id = :tenantId ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar relações por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UsersTenantsRoles> findByRoleId(Long roleId) {
        try {
            String sql = "SELECT * FROM users_tenants_roles WHERE role_id = :roleId ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("roleId", roleId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar relações por role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UsersTenantsRoles> findByUserIdAndTenantId(Long userId, Long tenantId) {
        try {
            String sql = "SELECT * FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar relações por usuário e tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UsersTenantsRoles> findByTenantIdAndRoleId(Long tenantId, Long roleId) {
        try {
            String sql = "SELECT * FROM users_tenants_roles WHERE tenant_id = :tenantId AND role_id = :roleId ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar relações por tenant e role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UsersTenantsRoles> findAll() {
        try {
            String sql = "SELECT * FROM users_tenants_roles ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todas as relações user-tenant-role: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByUserIdAndTenantIdAndRoleId(Long userId, Long tenantId, Long roleId) {
        try {
            String sql = "DELETE FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar relação user-tenant-role: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByUserId(Long userId) {
        try {
            String sql = "DELETE FROM users_tenants_roles WHERE user_id = :userId";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar relações por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByTenantId(Long tenantId) {
        try {
            String sql = "DELETE FROM users_tenants_roles WHERE tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar relações por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByRoleId(Long roleId) {
        try {
            String sql = "DELETE FROM users_tenants_roles WHERE role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource("roleId", roleId);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar relações por role: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByUserIdAndTenantId(Long userId, Long tenantId) {
        try {
            String sql = "DELETE FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar relações por usuário e tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByUserIdAndTenantIdAndRoleId(Long userId, Long tenantId, Long roleId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência da relação user-tenant-role: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByUserId(Long userId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE user_id = :userId";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de relações por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByTenantId(Long tenantId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de relações por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByRoleId(Long roleId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource("roleId", roleId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de relações por role: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByUserIdAndTenantId(Long userId, Long tenantId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de relações por usuário e tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar relações user-tenant-role: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByUserId(Long userId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE user_id = :userId";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar relações por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByTenantId(Long tenantId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar relações por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByRoleId(Long roleId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource("roleId", roleId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar relações por role: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByUserIdAndTenantId(Long userId, Long tenantId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar relações por usuário e tenant: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void clear() {
        try {
            String sql = "DELETE FROM users_tenants_roles";
            jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao limpar relações user-tenant-role: " + e.getMessage(), e);
        }
    }
}
