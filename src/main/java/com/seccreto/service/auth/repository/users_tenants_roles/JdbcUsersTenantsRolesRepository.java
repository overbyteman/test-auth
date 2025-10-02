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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de UsersTenantsRolesRepository usando JDBC + PostgreSQL.
 * Baseado na migração V7.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a multi-tenancy com relacionamentos complexos
 * - Suporte a UUIDs para alta performance
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
     * RowMapper otimizado
     */
    private static final RowMapper<UsersTenantsRoles> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        UsersTenantsRoles usersTenantsRoles = new UsersTenantsRoles();
        usersTenantsRoles.setUserId(rs.getObject("user_id", UUID.class));
        usersTenantsRoles.setTenantId(rs.getObject("tenant_id", UUID.class));
        usersTenantsRoles.setRoleId(rs.getObject("role_id", UUID.class));
        return usersTenantsRoles;
    };

    @Override
    @Transactional
    public UsersTenantsRoles save(UsersTenantsRoles usersTenantsRoles) {
        try {
            String sql = """
                INSERT INTO users_tenants_roles (user_id, tenant_id, role_id) 
                VALUES (:userId, :tenantId, :roleId)
                ON CONFLICT (user_id, tenant_id, role_id) DO NOTHING
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", usersTenantsRoles.getUserId())
                    .addValue("tenantId", usersTenantsRoles.getTenantId())
                    .addValue("roleId", usersTenantsRoles.getRoleId());

            namedParameterJdbcTemplate.update(sql, params);
            return usersTenantsRoles;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar relação user-tenant-role: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<UsersTenantsRoles> findByUserIdAndTenantIdAndRoleId(UUID userId, UUID tenantId, UUID roleId) {
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
    public List<UsersTenantsRoles> findByUserIdAndTenantId(UUID userId, UUID tenantId) {
        try {
            String sql = "SELECT * FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar roles por usuário e tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UsersTenantsRoles> findByTenantIdAndRoleId(UUID tenantId, UUID roleId) {
        try {
            String sql = "SELECT * FROM users_tenants_roles WHERE tenant_id = :tenantId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar usuários por tenant e role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UsersTenantsRoles> findByUserIdAndRoleId(UUID userId, UUID roleId) {
        try {
            String sql = "SELECT * FROM users_tenants_roles WHERE user_id = :userId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("roleId", roleId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar tenants por usuário e role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UsersTenantsRoles> findAll() {
        try {
            String sql = "SELECT * FROM users_tenants_roles";
            return jdbcTemplate.query(sql, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todas as relações user-tenant-role: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByUserIdAndTenantIdAndRoleId(UUID userId, UUID tenantId, UUID roleId) {
        try {
            String sql = "DELETE FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar relação user-tenant-role: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByUserIdAndTenantId(UUID userId, UUID tenantId) {
        try {
            String sql = "DELETE FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar roles por usuário e tenant: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByTenantIdAndRoleId(UUID tenantId, UUID roleId) {
        try {
            String sql = "DELETE FROM users_tenants_roles WHERE tenant_id = :tenantId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar usuários por tenant e role: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByUserIdAndRoleId(UUID userId, UUID roleId) {
        try {
            String sql = "DELETE FROM users_tenants_roles WHERE user_id = :userId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("roleId", roleId);
            
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar tenants por usuário e role: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByUserIdAndTenantIdAndRoleId(UUID userId, UUID tenantId, UUID roleId) {
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
    public boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de roles por usuário e tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByTenantIdAndRoleId(UUID tenantId, UUID roleId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE tenant_id = :tenantId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de usuários por tenant e role: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByUserIdAndRoleId(UUID userId, UUID roleId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE user_id = :userId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("roleId", roleId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de tenants por usuário e role: " + e.getMessage(), e);
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
    public long countByUserIdAndTenantId(UUID userId, UUID tenantId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE user_id = :userId AND tenant_id = :tenantId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar roles por usuário e tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByTenantIdAndRoleId(UUID tenantId, UUID roleId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE tenant_id = :tenantId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários por tenant e role: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByUserIdAndRoleId(UUID userId, UUID roleId) {
        try {
            String sql = "SELECT COUNT(1) FROM users_tenants_roles WHERE user_id = :userId AND role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("roleId", roleId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar tenants por usuário e role: " + e.getMessage(), e);
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