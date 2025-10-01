package com.seccreto.service.auth.repository.users;

import com.seccreto.service.auth.model.users.User;
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
 * Implementação de UserRepository usando JDBC + PostgreSQL.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a soft delete
 */
@Repository
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class JdbcUserRepository implements UserRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * RowMapper otimizado com tratamento de timezone e null safety
     */
    private static final RowMapper<User> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        
        // Tratamento seguro de timestamps com timezone
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return user;
    };

    @Override
    @Transactional
    public User save(User user) {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            String sql = """
                INSERT INTO users (name, email, created_at, updated_at, version, active) 
                VALUES (:name, :email, :createdAt, :updatedAt, 1, true) 
                RETURNING id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", user.getName())
                    .addValue("email", user.getEmail())
                    .addValue("createdAt", user.getCreatedAt())
                    .addValue("updatedAt", user.getUpdatedAt());

            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
            
            Long id = keyHolder.getKey().longValue();
            user.setId(id);
            return user;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            String sql = "SELECT * FROM users WHERE id = :id AND active = true";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            List<User> users = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return users.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar usuário por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findAll() {
        try {
            String sql = "SELECT * FROM users WHERE active = true ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todos os usuários: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findByName(String name) {
        try {
            String sql = """
                SELECT * FROM users 
                WHERE LOWER(name) LIKE LOWER(:name) AND active = true 
                ORDER BY name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("name", "%" + name + "%");
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar usuários por nome: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(:email) AND active = true";
            MapSqlParameterSource params = new MapSqlParameterSource("email", email);
            
            List<User> users = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return users.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar usuário por email: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public User update(User user) {
        try {
            String sql = """
                UPDATE users 
                SET name = :name, email = :email, updated_at = :updatedAt 
                WHERE id = :id AND active = true
                """;
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", user.getName())
                    .addValue("email", user.getEmail())
                    .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC))
                    .addValue("id", user.getId());

            int rows = namedParameterJdbcTemplate.update(sql, params);
            if (rows == 0) {
                throw new IllegalArgumentException("Usuário não encontrado para atualização");
            }
            return user;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao atualizar usuário: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        try {
            // Soft delete implementation
            String sql = "UPDATE users SET active = false, updated_at = :updatedAt WHERE id = :id AND active = true";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("updatedAt", LocalDateTime.now(ZoneOffset.UTC));
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        try {
            String sql = "SELECT COUNT(1) FROM users WHERE id = :id AND active = true";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            String sql = "SELECT COUNT(1) FROM users WHERE active = true";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void clear() {
        try {
            // Soft delete all users instead of truncating
            String sql = "UPDATE users SET active = false, updated_at = :updatedAt WHERE active = true";
            MapSqlParameterSource params = new MapSqlParameterSource("updatedAt", LocalDateTime.now(ZoneOffset.UTC));
            namedParameterJdbcTemplate.update(sql, params);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao limpar usuários: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findTopActiveUsers(int limit) {
        try {
            String sql = """
                SELECT u.* FROM users u
                LEFT JOIN sessions s ON u.id = s.user_id
                WHERE u.active = true
                GROUP BY u.id, u.username, u.email, u.password_hash, u.active, u.created_at, u.updated_at, u.version
                ORDER BY COUNT(s.id) DESC, u.created_at DESC
                LIMIT :limit
            """;
            MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar usuários mais ativos: " + e.getMessage(), e);
        }
    }

    @Override
    public long countActiveUsersInPeriod(String startDate, String endDate) {
        try {
            String sql = """
                SELECT COUNT(DISTINCT u.id) 
                FROM users u 
                LEFT JOIN sessions s ON u.id = s.user_id 
                WHERE u.active = true 
                AND s.created_at BETWEEN :startDate AND :endDate
            """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários ativos no período: " + e.getMessage(), e);
        }
    }

    @Override
    public long countUsersInPeriod(String startDate, String endDate) {
        try {
            String sql = """
                SELECT COUNT(1) 
                FROM users 
                WHERE created_at BETWEEN :startDate AND :endDate
            """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", startDate)
                    .addValue("endDate", endDate);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários no período: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findUsersByTenant(Long tenantId) {
        try {
            String sql = """
                SELECT DISTINCT u.* FROM users u
                JOIN users_tenants_roles utr ON u.id = utr.user_id
                WHERE utr.tenant_id = :tenantId AND u.active = true
                ORDER BY u.name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar usuários por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> search(String query) {
        try {
            String sql = """
                SELECT * FROM users 
                WHERE (LOWER(name) LIKE LOWER(:query) OR LOWER(email) LIKE LOWER(:query)) 
                AND active = true 
                ORDER BY name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("query", "%" + query + "%");
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar usuários: " + e.getMessage(), e);
        }
    }

    @Override
    public long countActiveUsers() {
        try {
            String sql = "SELECT COUNT(1) FROM users WHERE active = true";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários ativos: " + e.getMessage(), e);
        }
    }

    @Override
    public long countSuspendedUsers() {
        try {
            String sql = "SELECT COUNT(1) FROM users WHERE active = false";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários suspensos: " + e.getMessage(), e);
        }
    }

    @Override
    public long countUsersCreatedToday() {
        try {
            String sql = "SELECT COUNT(1) FROM users WHERE DATE(created_at) = CURRENT_DATE";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários criados hoje: " + e.getMessage(), e);
        }
    }

    @Override
    public long countUsersCreatedThisWeek() {
        try {
            String sql = "SELECT COUNT(1) FROM users WHERE created_at >= DATE_TRUNC('week', CURRENT_DATE)";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários criados esta semana: " + e.getMessage(), e);
        }
    }

    @Override
    public long countUsersCreatedThisMonth() {
        try {
            String sql = "SELECT COUNT(1) FROM users WHERE created_at >= DATE_TRUNC('month', CURRENT_DATE)";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários criados este mês: " + e.getMessage(), e);
        }
    }

    @Override
    public long countUsersByTenant(Long tenantId) {
        try {
            String sql = """
                SELECT COUNT(DISTINCT u.id) FROM users u
                JOIN users_tenants_roles utr ON u.id = utr.user_id
                WHERE utr.tenant_id = :tenantId AND u.active = true
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public long countActiveUsersByTenant(Long tenantId) {
        try {
            String sql = """
                SELECT COUNT(DISTINCT u.id) FROM users u
                JOIN users_tenants_roles utr ON u.id = utr.user_id
                WHERE utr.tenant_id = :tenantId AND u.active = true
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários ativos por tenant: " + e.getMessage(), e);
        }
    }
}
