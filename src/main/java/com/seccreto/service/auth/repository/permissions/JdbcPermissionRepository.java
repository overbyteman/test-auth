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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Implementação de PermissionRepository usando JDBC + PostgreSQL.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a versioning para optimistic locking
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
     * RowMapper otimizado com tratamento de timezone
     */
    private static final RowMapper<Permission> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Permission permission = new Permission();
        permission.setId(rs.getLong("id"));
        permission.setAction(rs.getString("action"));
        permission.setResource(rs.getString("resource"));
        
        // Tratamento seguro de timestamps com timezone
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        
        if (createdAt != null) {
            permission.setCreatedAt(createdAt.toLocalDateTime());
        }
        if (updatedAt != null) {
            permission.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        permission.setVersion(rs.getInt("version"));
        return permission;
    };

    @Override
    @Transactional
    public Permission save(Permission permission) {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            permission.setCreatedAt(now);
            permission.setUpdatedAt(now);
            permission.setVersion(1);

            String sql = """
                INSERT INTO permissions (action, resource, created_at, updated_at, version) 
                VALUES (:action, :resource, :createdAt, :updatedAt, :version) 
                RETURNING id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", permission.getAction())
                    .addValue("resource", permission.getResource())
                    .addValue("createdAt", permission.getCreatedAt())
                    .addValue("updatedAt", permission.getUpdatedAt())
                    .addValue("version", permission.getVersion());

            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
            
            Long id = keyHolder.getKey().longValue();
            permission.setId(id);
            return permission;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Permission> findById(Long id) {
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
            String sql = "SELECT * FROM permissions ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todas as permissões: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Permission> findByAction(String action) {
        try {
            String sql = """
                SELECT * FROM permissions 
                WHERE LOWER(action) LIKE LOWER(:action) 
                ORDER BY action, resource
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("action", "%" + action + "%");
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissões por ação: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Permission> findByResource(String resource) {
        try {
            String sql = """
                SELECT * FROM permissions 
                WHERE LOWER(resource) LIKE LOWER(:resource) 
                ORDER BY action, resource
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("resource", "%" + resource + "%");
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissões por recurso: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Permission> findByActionAndResource(String action, String resource) {
        try {
            String sql = """
                SELECT * FROM permissions 
                WHERE LOWER(action) LIKE LOWER(:action) AND LOWER(resource) LIKE LOWER(:resource)
                ORDER BY action, resource
                """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", "%" + action + "%")
                    .addValue("resource", "%" + resource + "%");
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissões por ação e recurso: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Permission> findByActionAndResourceExact(String action, String resource) {
        try {
            String sql = "SELECT * FROM permissions WHERE LOWER(action) = LOWER(:action) AND LOWER(resource) = LOWER(:resource)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", action)
                    .addValue("resource", resource);
            
            List<Permission> permissions = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return permissions.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar permissão por ação e recurso exatos: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Permission update(Permission permission) {
        try {
            String sql = """
                UPDATE permissions 
                SET action = :action, resource = :resource, updated_at = :updatedAt, version = :version
                WHERE id = :id AND version = :currentVersion
                """;
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", permission.getAction())
                    .addValue("resource", permission.getResource())
                    .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
                    .addValue("version", permission.getVersion() + 1)
                    .addValue("currentVersion", permission.getVersion())
                    .addValue("id", permission.getId());

            int rows = namedParameterJdbcTemplate.update(sql, params);
            if (rows == 0) {
                throw new IllegalArgumentException("Permissão não encontrada para atualização ou versão incorreta");
            }
            
            permission.setVersion(permission.getVersion() + 1);
            permission.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            return permission;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao atualizar permissão: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        try {
            String sql = "DELETE FROM permissions WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        try {
            String sql = "SELECT COUNT(1) FROM permissions WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência da permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByActionAndResource(String action, String resource) {
        try {
            String sql = "SELECT COUNT(1) FROM permissions WHERE LOWER(action) = LOWER(:action) AND LOWER(resource) = LOWER(:resource)";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", action)
                    .addValue("resource", resource);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência da permissão por ação e recurso: " + e.getMessage(), e);
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
    @Transactional
    public void clear() {
        try {
            String sql = "DELETE FROM permissions";
            jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao limpar permissões: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Permission> search(String query) {
        try {
            String sql = """
                SELECT id, action, resource, created_at, updated_at, version
                FROM permissions 
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
}
