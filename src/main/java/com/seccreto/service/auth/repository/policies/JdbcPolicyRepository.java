package com.seccreto.service.auth.repository.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;
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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de PolicyRepository usando JDBC + PostgreSQL.
 * Baseado na migração V8.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone, JSON e arrays
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices GIN para arrays e JSON
 * - Suporte a ABAC com condições JSON flexíveis
 * - Suporte a UUIDs para alta performance
 */
@Repository
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class JdbcPolicyRepository implements PolicyRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcPolicyRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.objectMapper = objectMapper;
    }

    /**
     * RowMapper otimizado com tratamento de timezone, JSON e arrays
     */
    private RowMapper<Policy> getRowMapper() {
        return (ResultSet rs, int rowNum) -> {
            try {
                Policy policy = new Policy();
                policy.setId(rs.getObject("id", UUID.class));
                policy.setName(rs.getString("name"));
                policy.setDescription(rs.getString("description"));

                // Tratamento de enum PolicyEffect
                String effectStr = rs.getString("effect");
                if (effectStr != null) {
                    policy.setEffect(PolicyEffect.valueOf(effectStr.toUpperCase()));
                }

                // Tratamento de arrays PostgreSQL
                String[] actions = (String[]) rs.getArray("actions").getArray();
                if (actions != null) {
                    policy.setActions(List.of(actions));
                }

                String[] resources = (String[]) rs.getArray("resources").getArray();
                if (resources != null) {
                    policy.setResources(List.of(resources));
                }

                // Tratamento seguro de JSON
                String conditionsJson = rs.getString("conditions");
                if (conditionsJson != null && !conditionsJson.trim().isEmpty()) {
                    JsonNode conditions = objectMapper.readTree(conditionsJson);
                    policy.setConditions(conditions);
                }

                // Tratamento seguro de timestamps com timezone
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    policy.setCreatedAt(createdAt.toLocalDateTime());
                }

                return policy;
            } catch (Exception e) {
                throw new RuntimeException("Erro ao mapear policy: " + e.getMessage(), e);
            }
        };
    }

    @Override
    @Transactional
    public Policy save(Policy policy) {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            policy.setCreatedAt(now);

            String sql = """
                INSERT INTO policies (name, description, effect, actions, resources, conditions, created_at) 
                VALUES (:name, :description, :effect, :actions, :resources, :conditions, :createdAt) 
                RETURNING id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", policy.getName())
                    .addValue("description", policy.getDescription())
                    .addValue("effect", policy.getEffect().toString().toLowerCase())
                    .addValue("actions", policy.getActions().toArray(new String[0]))
                    .addValue("resources", policy.getResources().toArray(new String[0]))
                    .addValue("conditions", policy.getConditions() != null ? objectMapper.writeValueAsString(policy.getConditions()) : null)
                    .addValue("createdAt", policy.getCreatedAt());

            UUID id = namedParameterJdbcTemplate.queryForObject(sql, params, UUID.class);
            policy.setId(id);
            return policy;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar policy: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar policy: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Policy> findById(UUID id) {
        try {
            String sql = "SELECT * FROM policies WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            List<Policy> policies = namedParameterJdbcTemplate.query(sql, params, getRowMapper());
            return policies.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policy por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Policy> findAll() {
        try {
            String sql = "SELECT * FROM policies ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todas as policies: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Policy> findByName(String name) {
        try {
            String sql = """
                SELECT * FROM policies 
                WHERE LOWER(name) LIKE LOWER(:name) 
                ORDER BY name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("name", "%" + name + "%");
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policies por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Policy> findByNameExact(String name) {
        try {
            String sql = "SELECT * FROM policies WHERE LOWER(name) = LOWER(:name)";
            MapSqlParameterSource params = new MapSqlParameterSource("name", name);
            
            List<Policy> policies = namedParameterJdbcTemplate.query(sql, params, getRowMapper());
            return policies.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policy por nome exato: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Policy> findByEffect(String effect) {
        try {
            String sql = "SELECT * FROM policies WHERE effect = :effect ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("effect", effect);
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policies por efeito: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Policy> findByEffectAndConditions(String effect, String conditions) {
        try {
            String sql = """
                SELECT * FROM policies 
                WHERE effect = :effect AND conditions::text LIKE :conditions 
                ORDER BY created_at DESC
                """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("effect", effect)
                    .addValue("conditions", "%" + conditions + "%");
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policies por efeito e condições: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Policy update(Policy policy) {
        try {
            String sql = """
                UPDATE policies 
                SET name = :name, description = :description, effect = :effect, 
                    actions = :actions, resources = :resources, conditions = :conditions
                WHERE id = :id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", policy.getId())
                    .addValue("name", policy.getName())
                    .addValue("description", policy.getDescription())
                    .addValue("effect", policy.getEffect().toString().toLowerCase())
                    .addValue("actions", policy.getActions().toArray(new String[0]))
                    .addValue("resources", policy.getResources().toArray(new String[0]))
                    .addValue("conditions", policy.getConditions() != null ? objectMapper.writeValueAsString(policy.getConditions()) : null);

            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            if (rowsAffected == 0) {
                throw new RuntimeException("Policy não encontrada para atualização");
            }
            return policy;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao atualizar policy: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar atualização de policy: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteById(UUID id) {
        try {
            String sql = "DELETE FROM policies WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar policy: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        try {
            String sql = "SELECT COUNT(1) FROM policies WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de policy: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        try {
            String sql = "SELECT COUNT(1) FROM policies WHERE LOWER(name) = LOWER(:name)";
            MapSqlParameterSource params = new MapSqlParameterSource("name", name);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência de policy por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            String sql = "SELECT COUNT(1) FROM policies";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar policies: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Policy> search(String query) {
        try {
            String sql = """
                SELECT * FROM policies 
                WHERE LOWER(name) LIKE LOWER(:query) 
                   OR LOWER(description) LIKE LOWER(:query)
                ORDER BY name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("query", "%" + query + "%");
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policies: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void clear() {
        try {
            String sql = "DELETE FROM policies";
            jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao limpar policies: " + e.getMessage(), e);
        }
    }
}