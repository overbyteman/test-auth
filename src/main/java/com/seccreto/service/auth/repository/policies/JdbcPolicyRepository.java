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

/**
 * Implementação de PolicyRepository usando JDBC + PostgreSQL.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone, JSON e arrays
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices GIN para arrays e JSON
 * - Suporte a versioning para optimistic locking
 * - Suporte a ABAC com condições JSON flexíveis
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
                policy.setId(rs.getLong("id"));
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
                Timestamp updatedAt = rs.getTimestamp("updated_at");

                if (createdAt != null) {
                    policy.setCreatedAt(createdAt.toLocalDateTime());
                }
                if (updatedAt != null) {
                    policy.setUpdatedAt(updatedAt.toLocalDateTime());
                }

                policy.setVersion(rs.getInt("version"));
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
            policy.setUpdatedAt(now);
            policy.setVersion(1);

            String sql = """
                INSERT INTO policies (name, description, effect, actions, resources, conditions, created_at, updated_at, version) 
                VALUES (:name, :description, :effect, :actions, :resources, :conditions, :createdAt, :updatedAt, :version) 
                RETURNING id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", policy.getName())
                    .addValue("description", policy.getDescription())
                    .addValue("effect", policy.getEffect() != null ? policy.getEffect().name().toLowerCase() : null)
                    .addValue("actions", policy.getActions() != null ? policy.getActions().toArray(new String[0]) : null)
                    .addValue("resources", policy.getResources() != null ? policy.getResources().toArray(new String[0]) : null)
                    .addValue("conditions", policy.getConditions() != null ? objectMapper.writeValueAsString(policy.getConditions()) : null)
                    .addValue("createdAt", policy.getCreatedAt())
                    .addValue("updatedAt", policy.getUpdatedAt())
                    .addValue("version", policy.getVersion());

            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
            
            Long id = keyHolder.getKey().longValue();
            policy.setId(id);
            return policy;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar policy: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar dados da policy: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Policy> findById(Long id) {
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
    public List<Policy> findByEffect(PolicyEffect effect) {
        try {
            String sql = "SELECT * FROM policies WHERE effect = :effect ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("effect", effect.name().toLowerCase());
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policies por efeito: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Policy> findByAction(String action) {
        try {
            String sql = "SELECT * FROM policies WHERE :action = ANY(actions) ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("action", action);
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policies por ação: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Policy> findByResource(String resource) {
        try {
            String sql = "SELECT * FROM policies WHERE :resource = ANY(resources) ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("resource", resource);
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policies por recurso: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Policy> findByActionAndResource(String action, String resource) {
        try {
            String sql = "SELECT * FROM policies WHERE :action = ANY(actions) AND :resource = ANY(resources) ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("action", action)
                    .addValue("resource", resource);
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar policies por ação e recurso: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Policy update(Policy policy) {
        try {
            String sql = """
                UPDATE policies 
                SET name = :name, description = :description, effect = :effect, 
                    actions = :actions, resources = :resources, conditions = :conditions, 
                    updated_at = :updatedAt, version = :version
                WHERE id = :id AND version = :currentVersion
                """;
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", policy.getName())
                    .addValue("description", policy.getDescription())
                    .addValue("effect", policy.getEffect() != null ? policy.getEffect().name().toLowerCase() : null)
                    .addValue("actions", policy.getActions() != null ? policy.getActions().toArray(new String[0]) : null)
                    .addValue("resources", policy.getResources() != null ? policy.getResources().toArray(new String[0]) : null)
                    .addValue("conditions", policy.getConditions() != null ? objectMapper.writeValueAsString(policy.getConditions()) : null)
                    .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
                    .addValue("version", policy.getVersion() + 1)
                    .addValue("currentVersion", policy.getVersion())
                    .addValue("id", policy.getId());

            int rows = namedParameterJdbcTemplate.update(sql, params);
            if (rows == 0) {
                throw new IllegalArgumentException("Policy não encontrada para atualização ou versão incorreta");
            }
            
            policy.setVersion(policy.getVersion() + 1);
            policy.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            return policy;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao atualizar policy: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar dados da policy: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        try {
            String sql = "DELETE FROM policies WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar policy: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        try {
            String sql = "SELECT COUNT(1) FROM policies WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência da policy: " + e.getMessage(), e);
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
            throw new RuntimeException("Erro ao verificar existência da policy por nome: " + e.getMessage(), e);
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
    public long countByEffect(PolicyEffect effect) {
        try {
            String sql = "SELECT COUNT(1) FROM policies WHERE effect = :effect";
            MapSqlParameterSource params = new MapSqlParameterSource("effect", effect.name().toLowerCase());
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar policies por efeito: " + e.getMessage(), e);
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

    @Override
    public List<Policy> search(String query) {
        try {
            String sql = """
                SELECT id, name, description, effect, actions, resources, conditions, created_at, updated_at, version
                FROM policies 
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
}
