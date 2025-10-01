package com.seccreto.service.auth.repository.sessions;

import com.seccreto.service.auth.model.sessions.Session;
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

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Implementação de SessionRepository usando JDBC + PostgreSQL.
 * 
 * Características de implementação sênior:
 * - Uso de NamedParameterJdbcTemplate para queries mais seguras
 * - RowMapper otimizado com tratamento de timezone e IP
 * - Transações declarativas
 * - Tratamento de exceções específicas
 * - Queries otimizadas com índices
 * - Suporte a limpeza automática de sessões expiradas
 */
@Repository
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class JdbcSessionRepository implements SessionRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public JdbcSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * RowMapper otimizado com tratamento de timezone e IP
     */
    private static final RowMapper<Session> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        Session session = new Session();
        session.setId(rs.getLong("id"));
        session.setUserId(rs.getLong("user_id"));
        session.setRefreshTokenHash(rs.getString("refresh_token_hash"));
        session.setUserAgent(rs.getString("user_agent"));
        
        // Tratamento seguro de IP address
        String ipAddressStr = rs.getString("ip_address");
        if (ipAddressStr != null && !ipAddressStr.trim().isEmpty()) {
            try {
                session.setIpAddress(InetAddress.getByName(ipAddressStr));
            } catch (Exception e) {
                // Log warning but don't fail
                System.err.println("Erro ao converter IP address: " + ipAddressStr);
            }
        }
        
        // Tratamento seguro de timestamps com timezone
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        
        if (expiresAt != null) {
            session.setExpiresAt(expiresAt.toLocalDateTime());
        }
        if (createdAt != null) {
            session.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return session;
    };

    @Override
    @Transactional
    public Session save(Session session) {
        try {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            session.setCreatedAt(now);

            String sql = """
                INSERT INTO sessions (user_id, refresh_token_hash, user_agent, ip_address, expires_at, created_at) 
                VALUES (:userId, :refreshTokenHash, :userAgent, :ipAddress, :expiresAt, :createdAt) 
                RETURNING id
                """;

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", session.getUserId())
                    .addValue("refreshTokenHash", session.getRefreshTokenHash())
                    .addValue("userAgent", session.getUserAgent())
                    .addValue("ipAddress", session.getIpAddress() != null ? session.getIpAddress().getHostAddress() : null)
                    .addValue("expiresAt", session.getExpiresAt())
                    .addValue("createdAt", session.getCreatedAt());

            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
            
            Long id = keyHolder.getKey().longValue();
            session.setId(id);
            return session;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao salvar sessão: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Session> findById(Long id) {
        try {
            String sql = "SELECT * FROM sessions WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            List<Session> sessions = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return sessions.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessão por ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> findAll() {
        try {
            String sql = "SELECT * FROM sessions ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar todas as sessões: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> findByUserId(Long userId) {
        try {
            String sql = "SELECT * FROM sessions WHERE user_id = :userId ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessões por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Session> findByRefreshTokenHash(String refreshTokenHash) {
        try {
            String sql = "SELECT * FROM sessions WHERE refresh_token_hash = :refreshTokenHash";
            MapSqlParameterSource params = new MapSqlParameterSource("refreshTokenHash", refreshTokenHash);
            
            List<Session> sessions = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
            return sessions.stream().findFirst();
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessão por refresh token: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> findByUserAgent(String userAgent) {
        try {
            String sql = "SELECT * FROM sessions WHERE user_agent = :userAgent ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("userAgent", userAgent);
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessões por user agent: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> findByIpAddress(InetAddress ipAddress) {
        try {
            String sql = "SELECT * FROM sessions WHERE ip_address = :ipAddress ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("ipAddress", ipAddress.getHostAddress());
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessões por IP: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> findExpiredSessions() {
        try {
            String sql = "SELECT * FROM sessions WHERE expires_at < :now ORDER BY expires_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("now", LocalDateTime.now(ZoneOffset.UTC));
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessões expiradas: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> findValidSessions() {
        try {
            String sql = "SELECT * FROM sessions WHERE expires_at >= :now ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("now", LocalDateTime.now(ZoneOffset.UTC));
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessões válidas: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> findByUserIdAndValid(Long userId) {
        try {
            String sql = "SELECT * FROM sessions WHERE user_id = :userId AND expires_at >= :now ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("now", LocalDateTime.now(ZoneOffset.UTC));
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessões válidas por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Session update(Session session) {
        try {
            String sql = """
                UPDATE sessions 
                SET user_id = :userId, refresh_token_hash = :refreshTokenHash, 
                    user_agent = :userAgent, ip_address = :ipAddress, expires_at = :expiresAt
                WHERE id = :id
                """;
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", session.getUserId())
                    .addValue("refreshTokenHash", session.getRefreshTokenHash())
                    .addValue("userAgent", session.getUserAgent())
                    .addValue("ipAddress", session.getIpAddress() != null ? session.getIpAddress().getHostAddress() : null)
                    .addValue("expiresAt", session.getExpiresAt())
                    .addValue("id", session.getId());

            int rows = namedParameterJdbcTemplate.update(sql, params);
            if (rows == 0) {
                throw new IllegalArgumentException("Sessão não encontrada para atualização");
            }
            return session;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao atualizar sessão: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        try {
            String sql = "DELETE FROM sessions WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar sessão: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteByUserId(Long userId) {
        try {
            String sql = "DELETE FROM sessions WHERE user_id = :userId";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar sessões por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean deleteExpiredSessions() {
        try {
            String sql = "DELETE FROM sessions WHERE expires_at < :now";
            MapSqlParameterSource params = new MapSqlParameterSource("now", LocalDateTime.now(ZoneOffset.UTC));
            
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao deletar sessões expiradas: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        try {
            String sql = "SELECT COUNT(1) FROM sessions WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource("id", id);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência da sessão: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByRefreshTokenHash(String refreshTokenHash) {
        try {
            String sql = "SELECT COUNT(1) FROM sessions WHERE refresh_token_hash = :refreshTokenHash";
            MapSqlParameterSource params = new MapSqlParameterSource("refreshTokenHash", refreshTokenHash);
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao verificar existência do refresh token: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            String sql = "SELECT COUNT(1) FROM sessions";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByUserId(Long userId) {
        try {
            String sql = "SELECT COUNT(1) FROM sessions WHERE user_id = :userId";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public long countValidSessions() {
        try {
            String sql = "SELECT COUNT(1) FROM sessions WHERE expires_at >= :now";
            MapSqlParameterSource params = new MapSqlParameterSource("now", LocalDateTime.now(ZoneOffset.UTC));
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões válidas: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void clear() {
        try {
            String sql = "DELETE FROM sessions";
            jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao limpar sessões: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> findActiveSessions() {
        try {
            String sql = "SELECT * FROM sessions WHERE expires_at >= :now ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource("now", LocalDateTime.now(ZoneOffset.UTC));
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessões ativas: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> findActiveSessionsByUser(Long userId) {
        try {
            String sql = "SELECT * FROM sessions WHERE user_id = :userId AND expires_at >= :now ORDER BY created_at DESC";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("now", LocalDateTime.now(ZoneOffset.UTC));
            return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessões ativas por usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public long countActiveSessions() {
        try {
            String sql = "SELECT COUNT(1) FROM sessions WHERE expires_at >= :now";
            MapSqlParameterSource params = new MapSqlParameterSource("now", LocalDateTime.now(ZoneOffset.UTC));
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões ativas: " + e.getMessage(), e);
        }
    }

    @Override
    public long countExpiredSessions() {
        try {
            String sql = "SELECT COUNT(1) FROM sessions WHERE expires_at < :now";
            MapSqlParameterSource params = new MapSqlParameterSource("now", LocalDateTime.now(ZoneOffset.UTC));
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões expiradas: " + e.getMessage(), e);
        }
    }

    @Override
    public long countSessionsToday() {
        try {
            LocalDateTime startOfDay = LocalDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay();
            String sql = "SELECT COUNT(1) FROM sessions WHERE created_at >= :startOfDay";
            MapSqlParameterSource params = new MapSqlParameterSource("startOfDay", startOfDay);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões de hoje: " + e.getMessage(), e);
        }
    }

    @Override
    public long countSessionsThisWeek() {
        try {
            LocalDateTime startOfWeek = LocalDateTime.now(ZoneOffset.UTC).minusDays(7);
            String sql = "SELECT COUNT(1) FROM sessions WHERE created_at >= :startOfWeek";
            MapSqlParameterSource params = new MapSqlParameterSource("startOfWeek", startOfWeek);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões desta semana: " + e.getMessage(), e);
        }
    }

    @Override
    public long countSessionsThisMonth() {
        try {
            LocalDateTime startOfMonth = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);
            String sql = "SELECT COUNT(1) FROM sessions WHERE created_at >= :startOfMonth";
            MapSqlParameterSource params = new MapSqlParameterSource("startOfMonth", startOfMonth);
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões deste mês: " + e.getMessage(), e);
        }
    }

    @Override
    public long countSessionsInPeriod(String startDate, String endDate) {
        try {
            String sql = "SELECT COUNT(1) FROM sessions WHERE created_at BETWEEN :startDate AND :endDate";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", LocalDateTime.parse(startDate + "T00:00:00"))
                    .addValue("endDate", LocalDateTime.parse(endDate + "T23:59:59"));
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões no período: " + e.getMessage(), e);
        }
    }

    @Override
    public long countActiveSessionsInPeriod(String startDate, String endDate) {
        try {
            String sql = "SELECT COUNT(1) FROM sessions WHERE created_at BETWEEN :startDate AND :endDate AND expires_at >= :now";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", LocalDateTime.parse(startDate + "T00:00:00"))
                    .addValue("endDate", LocalDateTime.parse(endDate + "T23:59:59"))
                    .addValue("now", LocalDateTime.now(ZoneOffset.UTC));
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao contar sessões ativas no período: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Session> search(String ipAddress, String userAgent, Long userId) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM sessions WHERE 1=1");
            MapSqlParameterSource params = new MapSqlParameterSource();

            if (ipAddress != null && !ipAddress.trim().isEmpty()) {
                sql.append(" AND CAST(ip_address AS TEXT) LIKE :ipAddress");
                params.addValue("ipAddress", "%" + ipAddress + "%");
            }

            if (userAgent != null && !userAgent.trim().isEmpty()) {
                sql.append(" AND LOWER(user_agent) LIKE LOWER(:userAgent)");
                params.addValue("userAgent", "%" + userAgent + "%");
            }

            if (userId != null) {
                sql.append(" AND user_id = :userId");
                params.addValue("userId", userId);
            }

            sql.append(" ORDER BY created_at DESC");

            return namedParameterJdbcTemplate.query(sql.toString(), params, ROW_MAPPER);
        } catch (DataAccessException e) {
            throw new RuntimeException("Erro ao buscar sessões: " + e.getMessage(), e);
        }
    }

    @Override
    public String findLastLoginByUser(Long userId) {
        try {
            String sql = "SELECT created_at FROM sessions WHERE user_id = :userId ORDER BY created_at DESC LIMIT 1";
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            LocalDateTime lastLogin = namedParameterJdbcTemplate.queryForObject(sql, params, LocalDateTime.class);
            return lastLogin != null ? lastLogin.toString() : null;
        } catch (DataAccessException e) {
            return null; // Retorna null se não encontrar nenhuma sessão
        }
    }
}
