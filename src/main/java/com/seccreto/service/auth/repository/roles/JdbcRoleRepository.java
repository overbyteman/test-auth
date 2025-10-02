package com.seccreto.service.auth.repository.roles;

import com.seccreto.service.auth.model.roles.Role;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de RoleRepository usando JDBC + PostgreSQL.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a UUIDs para alta performance
 */
@Repository
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class JdbcRoleRepository implements RoleRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public JdbcRoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * RowMapper otimizado
     */
    private static final RowMapper<Role> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Role role = new Role();
        role.setId(rs.getObject("id", UUID.class));
        role.setName(rs.getString("name"));
        role.setDescription(rs.getString("description"));
        return role;
    };

    @Override
    @Transactional
    public Role save(Role role) {
        try {
            String sql = """
                INSERT INTO roles (name, description) 
                VALUES (:name, :description) 
                RETURNING id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", role.getName())
                    .addValue("description", role.getDescription());

            UUID id = namedParameterJdbcTemplate.queryForObject(sql, params, UUID.class);
            role.setId(id);
            return role;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar role: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Role> findById(UUID id) {
        try {
            String sql = "SELECT * FROM roles WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            List<Role> roles = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return roles.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar role por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Role> findAll() {
        try {
            String sql = "SELECT * FROM roles ORDER BY name";
            return jdbcTemplate.query(sql, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todos os roles: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Role> findByName(String name) {
        try {
            String sql = """
                SELECT * FROM roles 
                WHERE LOWER(name) LIKE LOWER(:name) 
                ORDER BY name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("name", "%" + name + "%");
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar roles por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Role> findByNameExact(String name) {
        try {
            String sql = "SELECT * FROM roles WHERE LOWER(name) = LOWER(:name)";
            MapSqlParameterSource params = new MapSqlParameterSource("name", name);
            
            List<Role> roles = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return roles.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar role por nome exato: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Role update(Role role) {
        try {
            String sql = """
                UPDATE roles 
                SET name = :name, description = :description
                WHERE id = :id
                """;
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", role.getName())
                    .addValue("description", role.getDescription())
                    .addValue("id", role.getId());

            int rows = namedParameterJdbcTemplate.update(sql, params);
            if (rows == 0) {
                throw new IllegalArgumentException("Role não encontrado para atualização");
            }
            
            return role;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao atualizar role: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteById(UUID id) {
        try {
            String sql = "DELETE FROM roles WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar role: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        try {
            String sql = "SELECT COUNT(1) FROM roles WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência do role: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        try {
            String sql = "SELECT COUNT(1) FROM roles WHERE LOWER(name) = LOWER(:name)";
            MapSqlParameterSource params = new MapSqlParameterSource("name", name);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência do role por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            String sql = "SELECT COUNT(1) FROM roles";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar roles: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void clear() {
        try {
            String sql = "DELETE FROM roles";
            jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao limpar roles: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Role> search(String query) {
        try {
            String sql = """
                SELECT * FROM roles 
                WHERE LOWER(name) LIKE LOWER(:query) 
                   OR LOWER(description) LIKE LOWER(:query)
                ORDER BY name
            """;
            MapSqlParameterSource params = new MapSqlParameterSource("query", "%" + query + "%");
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar roles: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Long> getRoleDistribution() {
        try {
            String sql = """
                SELECT r.name, COUNT(utr.role_id) as count
                FROM roles r
                LEFT JOIN users_tenants_roles utr ON r.id = utr.role_id
                GROUP BY r.id, r.name
                ORDER BY count DESC
            """;
            return jdbcTemplate.query(sql, rs -> {
                Map<String, Long> distribution = new HashMap<>();
                while (rs.next()) {
                    distribution.put(rs.getString("name"), rs.getLong("count"));
                }
                return distribution;
            });
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao obter distribuição de roles: " + e.getMessage(), e);
        }
    }
}