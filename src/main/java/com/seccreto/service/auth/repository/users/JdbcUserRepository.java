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
import java.util.UUID;

/**
 * Implementação de UserRepository usando JDBC + PostgreSQL.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a UUIDs para alta performance
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
     * RowMapper otimizado com tratamento de timezone e UUID
     */
    private static final RowMapper<User> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        User user = new User();
        user.setId(rs.getObject("id", UUID.class));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setIsActive(rs.getBoolean("is_active"));
        user.setEmailVerificationToken(rs.getString("email_verification_token"));
        
        // Tratamento seguro de timestamps com timezone
        Timestamp emailVerifiedAt = rs.getTimestamp("email_verified_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        
        if (emailVerifiedAt != null) {
            user.setEmailVerifiedAt(emailVerifiedAt.toLocalDateTime());
        }
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
                INSERT INTO users (name, email, password_hash, is_active, email_verification_token, email_verified_at, created_at, updated_at) 
                VALUES (:name, :email, :passwordHash, :isActive, :emailVerificationToken, :emailVerifiedAt, :createdAt, :updatedAt) 
                RETURNING id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", user.getName())
                    .addValue("email", user.getEmail())
                    .addValue("passwordHash", user.getPasswordHash())
                    .addValue("isActive", user.getIsActive())
                    .addValue("emailVerificationToken", user.getEmailVerificationToken())
                    .addValue("emailVerifiedAt", user.getEmailVerifiedAt())
                    .addValue("createdAt", user.getCreatedAt())
                    .addValue("updatedAt", user.getUpdatedAt());

            UUID id = namedParameterJdbcTemplate.queryForObject(sql, params, UUID.class);
            user.setId(id);
            return user;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        try {
            String sql = "SELECT * FROM users WHERE id = :id";
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
            String sql = "SELECT * FROM users ORDER BY created_at DESC";
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
                WHERE LOWER(name) LIKE LOWER(:name) 
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
            String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(:email)";
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
                SET name = :name, email = :email, password_hash = :passwordHash, is_active = :isActive, 
                    email_verification_token = :emailVerificationToken, email_verified_at = :emailVerifiedAt, 
                    updated_at = :updatedAt 
                WHERE id = :id
                """;
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("name", user.getName())
                    .addValue("email", user.getEmail())
                    .addValue("passwordHash", user.getPasswordHash())
                    .addValue("isActive", user.getIsActive())
                    .addValue("emailVerificationToken", user.getEmailVerificationToken())
                    .addValue("emailVerifiedAt", user.getEmailVerifiedAt())
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
    public boolean deleteById(UUID id) {
        try {
            String sql = "DELETE FROM users WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(UUID id) {
        try {
            String sql = "SELECT COUNT(1) FROM users WHERE id = :id";
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
            String sql = "SELECT COUNT(1) FROM users";
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
            String sql = "DELETE FROM users";
            jdbcTemplate.update(sql);
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
                GROUP BY u.id, u.name, u.email, u.password_hash, u.is_active, u.email_verification_token, u.email_verified_at, u.created_at, u.updated_at
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
                WHERE s.created_at BETWEEN :startDate AND :endDate
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
    public List<User> findUsersByTenant(UUID tenantId) {
        try {
            String sql = """
                SELECT DISTINCT u.* FROM users u
                JOIN users_tenants_roles utr ON u.id = utr.user_id
                WHERE utr.tenant_id = :tenantId
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
            String sql = "SELECT COUNT(1) FROM users WHERE is_active = true";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários ativos: " + e.getMessage(), e);
        }
    }

    @Override
    public long countSuspendedUsers() {
        try {
            String sql = "SELECT COUNT(1) FROM users WHERE is_active = false";
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
    public long countUsersByTenant(UUID tenantId) {
        try {
            String sql = """
                SELECT COUNT(DISTINCT u.id) FROM users u
                JOIN users_tenants_roles utr ON u.id = utr.user_id
                WHERE utr.tenant_id = :tenantId
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários por tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public long countActiveUsersByTenant(UUID tenantId) {
        try {
            String sql = """
                SELECT COUNT(DISTINCT u.id) FROM users u
                JOIN users_tenants_roles utr ON u.id = utr.user_id
                WHERE utr.tenant_id = :tenantId AND u.is_active = true
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar usuários ativos por tenant: " + e.getMessage(), e);
        }
    }
}