package com.seccreto.service.auth.repository.sessions;

import com.seccreto.service.auth.model.sessions.Session;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração de repositório para a entidade Session, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface SessionRepository {
    Session save(Session session);
    Optional<Session> findById(UUID id);
    List<Session> findAll();
    List<Session> findByUserId(UUID userId);
    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);
    List<Session> findByUserAgent(String userAgent);
    List<Session> findByIpAddress(InetAddress ipAddress);
    List<Session> findExpiredSessions();
    List<Session> findValidSessions();
    List<Session> findByUserIdAndValid(UUID userId);
    Session update(Session session);
    boolean deleteById(UUID id);
    boolean deleteByUserId(UUID userId);
    boolean deleteExpiredSessions();
    boolean existsById(UUID id);
    boolean existsByRefreshTokenHash(String refreshTokenHash);
    long count();
    long countByUserId(UUID userId);
    long countValidSessions();
    void clear();
    
    // Métodos adicionais para controllers
    List<Session> findActiveSessions();
    List<Session> findActiveSessionsByUser(UUID userId);
    long countActiveSessions();
    long countExpiredSessions();
    long countSessionsToday();
    long countSessionsThisWeek();
    long countSessionsThisMonth();
    long countSessionsInPeriod(String startDate, String endDate);
    long countActiveSessionsInPeriod(String startDate, String endDate);
    List<Session> search(String ipAddress, String userAgent, UUID userId);
    String findLastLoginByUser(UUID userId);
}