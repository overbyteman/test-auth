package com.seccreto.service.auth.repository.permissions;

import com.seccreto.service.auth.model.permissions.Permission;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de PermissionRepository usando JDBC + PostgreSQL.
 * Baseado na migração V5.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a UUIDs para alta performance
 * - Busca por action e resource combinados
 */
@Repository
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class JdbcPermissionRepository implements PermissionRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public JdbcPermissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * RowMapper otimizado
     */
    private static final RowMapper<Permission> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Permission permission = new Permission();
        permission.setId(rs.getObject("id", UUID.class));
        permission.setAction(rs.getString("action"));
        permission.setResource(rs.getString("resource"));
        return permission;
    };

    @Override
    @Transactional
    public Permission save(Permission permission) {
        try {
            String sql = """
                INSERT INTO permissions (action, resource) 
                VALUES (:action, :resource) 
                RETURNING id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", permission.getAction())
                    .addValue("resource", permission.getResource());

            UUID id = namedParameterJdbcTemplate.queryForObject(sql, params, UUID.class);
            permission.setId(id);
            return permission;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Permission> findById(UUID id) {
        try {
            String sql = "SELECT * FROM permissions WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            List<Permission> permissions = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return permissions.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissão por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Permission> findAll() {
        try {
            String sql = "SELECT * FROM permissions ORDER BY action, resource";
            return jdbcTemplate.query(sql, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todas as permissões: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Permission> findByAction(String action) {
        try {
            String sql = "SELECT * FROM permissions WHERE action = :action ORDER BY resource";
            MapSqlParameterSource params = new MapSqlParameterSource("action", action);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissões por ação: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Permission> findByResource(String resource) {
        try {
            String sql = "SELECT * FROM permissions WHERE resource = :resource ORDER BY action";
            MapSqlParameterSource params = new MapSqlParameterSource("resource", resource);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissões por recurso: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Permission> findByActionAndResource(String action, String resource) {
        try {
            String sql = "SELECT * FROM permissions WHERE action = :action AND resource = :resource";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", action)
                    .addValue("resource", resource);
            
            List<Permission> permissions = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return permissions.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissão por ação e recurso: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Permission update(Permission permission) {
        try {
            String sql = """
                UPDATE permissions 
                SET action = :action, resource = :resource
                WHERE id = :id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", permission.getId())
                    .addValue("action", permission.getAction())
                    .addValue("resource", permission.getResource());

            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            if (rowsAffected == 0) {
                throw new RuntimeException("Permissão não encontrada para atualização");
            }
            return permission;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao atualizar permissão: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteById(UUID id) {
        try {
            String sql = "DELETE FROM permissions WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        try {
            String sql = "SELECT COUNT(1) FROM permissions WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByActionAndResource(String action, String resource) {
        try {
            String sql = "SELECT COUNT(1) FROM permissions WHERE action = :action AND resource = :resource";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", action)
                    .addValue("resource", resource);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de permissão por ação e recurso: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            String sql = "SELECT COUNT(1) FROM permissions";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar permissões: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Permission> search(String query) {
        try {
            String sql = """
                SELECT * FROM permissions 
                WHERE LOWER(action) LIKE LOWER(:query) 
                   OR LOWER(resource) LIKE LOWER(:query)
                ORDER BY action, resource
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("query", "%" + query + "%");
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissões: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try {
            String sql = "DELETE FROM permissions";
            jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao limpar permissões: " + e.getMessage(), e);
        }
    }
}