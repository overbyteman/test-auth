package com.seccreto.service.auth.repository.tenants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seccreto.service.auth.model.tenants.Tenant;
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
 * Implementação de TenantRepository usando JDBC + PostgreSQL.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone e JSON
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a configuração JSON flexível
 */
@Repository
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class JdbcTenantRepository implements TenantRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcTenantRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.objectMapper = objectMapper;
    }

    /**
     * RowMapper otimizado com tratamento de timezone e JSON
     */
    private RowMapper<Tenant> getRowMapper() {
        return (ResultSet rs, int rowNum) -> {
            try {
                Tenant tenant = new Tenant();
                tenant.setId(rs.getLong("id"));
                tenant.setName(rs.getString("name"));
                tenant.setDescription(rs.getString("description"));
                tenant.setDomain(rs.getString("domain"));
                tenant.setActive(rs.getBoolean("active"));

                // Tratamento seguro de JSON
                String configJson = rs.getString("config");
                if (configJson != null && !configJson.trim().isEmpty()) {
                    JsonNode config = objectMapper.readTree(configJson);
                    tenant.setConfig(config);
                }

                // Tratamento seguro de timestamps com timezone
                Timestamp createdAt = rs.getTimestamp("created_at");
                Timestamp updatedAt = rs.getTimestamp("updated_at");

                if (createdAt != null) {
                    tenant.setCreatedAt(createdAt.toLocalDateTime());
                }
                if (updatedAt != null) {
                    tenant.setUpdatedAt(updatedAt.toLocalDateTime());
                }

                tenant.setVersion(rs.getInt("version"));
                return tenant;
            } catch (Exception e) {
                throw new RuntimeException("Erro ao mapear tenant: " + e.getMessage(), e);
            }
        };
    }

    @Override
    @Transactional
    public Tenant save(Tenant tenant) {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            tenant.setCreatedAt(now);
            tenant.setUpdatedAt(now);
            tenant.setVersion(1);

            String sql = """
                INSERT INTO tenants (name, description, domain, active, config, created_at, updated_at, version) 
                VALUES (:name, :description, :domain, :active, :config, :createdAt, :updatedAt, :version) 
                RETURNING id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", tenant.getName())
                    .addValue("description", tenant.getDescription())
                    .addValue("domain", tenant.getDomain())
                    .addValue("active", tenant.getActive())
                    .addValue("config", tenant.getConfig() != null ? objectMapper.writeValueAsString(tenant.getConfig()) : null)
                    .addValue("createdAt", tenant.getCreatedAt())
                    .addValue("updatedAt", tenant.getUpdatedAt())
                    .addValue("version", tenant.getVersion());

            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
            
            Long id = keyHolder.getKey().longValue();
            tenant.setId(id);
            return tenant;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar tenant: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar configuração JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        try {
            String sql = "SELECT * FROM tenants WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            List<Tenant> tenants = namedParameterJdbcTemplate.query(sql, params, getRowMapper());
            return tenants.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar tenant por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Tenant> findAll() {
        try {
            String sql = "SELECT * FROM tenants ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todos os tenants: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Tenant> findByName(String name) {
        try {
            String sql = """
                SELECT * FROM tenants 
                WHERE LOWER(name) LIKE LOWER(:name) 
                ORDER BY name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("name", "%" + name + "%");
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar tenants por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Tenant> findByNameExact(String name) {
        try {
            String sql = "SELECT * FROM tenants WHERE LOWER(name) = LOWER(:name)";
            MapSqlParameterSource params = new MapSqlParameterSource("name", name);
            
            List<Tenant> tenants = namedParameterJdbcTemplate.query(sql, params, getRowMapper());
            return tenants.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar tenant por nome exato: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Tenant update(Tenant tenant) {
        try {
            String sql = """
                UPDATE tenants 
                SET name = :name, description = :description, domain = :domain, active = :active, config = :config, updated_at = :updatedAt, version = :version
                WHERE id = :id AND version = :currentVersion
                """;
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", tenant.getName())
                    .addValue("description", tenant.getDescription())
                    .addValue("domain", tenant.getDomain())
                    .addValue("active", tenant.getActive())
                    .addValue("config", tenant.getConfig() != null ? objectMapper.writeValueAsString(tenant.getConfig()) : null)
                    .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
                    .addValue("version", tenant.getVersion() + 1)
                    .addValue("currentVersion", tenant.getVersion())
                    .addValue("id", tenant.getId());

            int rows = namedParameterJdbcTemplate.update(sql, params);
            if (rows == 0) {
                throw new IllegalArgumentException("Tenant não encontrado para atualização ou versão incorreta");
            }
            
            tenant.setVersion(tenant.getVersion() + 1);
            tenant.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            return tenant;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao atualizar tenant: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar configuração JSON: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        try {
            String sql = "DELETE FROM tenants WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        try {
            String sql = "SELECT COUNT(1) FROM tenants WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência do tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        try {
            String sql = "SELECT COUNT(1) FROM tenants WHERE LOWER(name) = LOWER(:name)";
            MapSqlParameterSource params = new MapSqlParameterSource("name", name);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência do tenant por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            String sql = "SELECT COUNT(1) FROM tenants";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar tenants: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void clear() {
        try {
            String sql = "DELETE FROM tenants";
            jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao limpar tenants: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByActive(Boolean active) {
        try {
            String sql = "SELECT COUNT(1) FROM tenants WHERE active = :active";
            MapSqlParameterSource params = new MapSqlParameterSource("active", active);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar tenants por status: " + e.getMessage(), e);
        }
    }

    @Override
    public long countCreatedToday() {
        try {
            String sql = "SELECT COUNT(1) FROM tenants WHERE DATE(created_at) = CURRENT_DATE";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar tenants criados hoje: " + e.getMessage(), e);
        }
    }

    @Override
    public long countCreatedThisWeek() {
        try {
            String sql = "SELECT COUNT(1) FROM tenants WHERE created_at >= DATE_TRUNC('week', CURRENT_DATE)";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar tenants criados esta semana: " + e.getMessage(), e);
        }
    }

    @Override
    public long countCreatedThisMonth() {
        try {
            String sql = "SELECT COUNT(1) FROM tenants WHERE created_at >= DATE_TRUNC('month', CURRENT_DATE)";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar tenants criados este mês: " + e.getMessage(), e);
        }
    }

    @Override
    public long countInPeriod(String startDate, String endDate) {
        try {
            String sql = "SELECT COUNT(1) FROM tenants WHERE created_at BETWEEN :startDate AND :endDate";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar tenants no período: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Tenant> search(String query) {
        try {
            String sql = "SELECT * FROM tenants WHERE LOWER(name) LIKE LOWER(:query) OR LOWER(description) LIKE LOWER(:query) OR LOWER(domain) LIKE LOWER(:query)";
            MapSqlParameterSource params = new MapSqlParameterSource("query", "%" + query + "%");
            return namedParameterJdbcTemplate.query(sql, params, getRowMapper());
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar tenants: " + e.getMessage(), e);
        }
    }
}
