package com.seccreto.service.auth.repository.roles_permissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
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
 * Implementação de RolesPermissionsRepository usando JDBC + PostgreSQL.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a relacionamentos many-to-many
 */
@Repository
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class JdbcRolesPermissionsRepository implements RolesPermissionsRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public JdbcRolesPermissionsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * RowMapper otimizado com tratamento de timezone
     */
    private static final RowMapper<RolesPermissions> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        RolesPermissions rolesPermissions = new RolesPermissions();
        rolesPermissions.setRoleId(rs.getLong("role_id"));
        rolesPermissions.setPermissionId(rs.getLong("permission_id"));
        
        // Tratamento seguro de timestamps com timezone
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            rolesPermissions.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return rolesPermissions;
    };

    @Override
    @Transactional
    public RolesPermissions save(RolesPermissions rolesPermissions) {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            rolesPermissions.setCreatedAt(now);

            String sql = """
                INSERT INTO roles_permissions (role_id, permission_id, created_at) 
                VALUES (:roleId, :permissionId, :createdAt)
                ON CONFLICT (role_id, permission_id) DO NOTHING
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("roleId", rolesPermissions.getRoleId())
                    .addValue("permissionId", rolesPermissions.getPermissionId())
                    .addValue("createdAt", rolesPermissions.getCreatedAt());

            namedParameterJdbcTemplate.update(sql, params);
            return rolesPermissions;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar relação role-permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<RolesPermissions> findByRoleIdAndPermissionId(Long roleId, Long permissionId) {
        try {
            String sql = "SELECT * FROM roles_permissions WHERE role_id = :roleId AND permission_id = :permissionId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("roleId", roleId)
                    .addValue("permissionId", permissionId);
            
            List<RolesPermissions> relations = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return relations.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar relação role-permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RolesPermissions> findByRoleId(Long roleId) {
        try {
            String sql = "SELECT * FROM roles_permissions WHERE role_id = :roleId ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("roleId", roleId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissões por role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RolesPermissions> findByPermissionId(Long permissionId) {
        try {
            String sql = "SELECT * FROM roles_permissions WHERE permission_id = :permissionId ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("permissionId", permissionId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar roles por permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RolesPermissions> findAll() {
        try {
            String sql = "SELECT * FROM roles_permissions ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todas as relações role-permissão: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByRoleIdAndPermissionId(Long roleId, Long permissionId) {
        try {
            String sql = "DELETE FROM roles_permissions WHERE role_id = :roleId AND permission_id = :permissionId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("roleId", roleId)
                    .addValue("permissionId", permissionId);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar relação role-permissão: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByRoleId(Long roleId) {
        try {
            String sql = "DELETE FROM roles_permissions WHERE role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource("roleId", roleId);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar permissões por role: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByPermissionId(Long permissionId) {
        try {
            String sql = "DELETE FROM roles_permissions WHERE permission_id = :permissionId";
            MapSqlParameterSource params = new MapSqlParameterSource("permissionId", permissionId);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar roles por permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId) {
        try {
            String sql = "SELECT COUNT(1) FROM roles_permissions WHERE role_id = :roleId AND permission_id = :permissionId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("roleId", roleId)
                    .addValue("permissionId", permissionId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência da relação role-permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByRoleId(Long roleId) {
        try {
            String sql = "SELECT COUNT(1) FROM roles_permissions WHERE role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource("roleId", roleId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de permissões por role: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByPermissionId(Long permissionId) {
        try {
            String sql = "SELECT COUNT(1) FROM roles_permissions WHERE permission_id = :permissionId";
            MapSqlParameterSource params = new MapSqlParameterSource("permissionId", permissionId);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de roles por permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            String sql = "SELECT COUNT(1) FROM roles_permissions";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar relações role-permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByRoleId(Long roleId) {
        try {
            String sql = "SELECT COUNT(1) FROM roles_permissions WHERE role_id = :roleId";
            MapSqlParameterSource params = new MapSqlParameterSource("roleId", roleId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar permissões por role: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByPermissionId(Long permissionId) {
        try {
            String sql = "SELECT COUNT(1) FROM roles_permissions WHERE permission_id = :permissionId";
            MapSqlParameterSource params = new MapSqlParameterSource("permissionId", permissionId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar roles por permissão: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void clear() {
        try {
            String sql = "DELETE FROM roles_permissions";
            jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao limpar relações role-permissão: " + e.getMessage(), e);
        }
    }
}
