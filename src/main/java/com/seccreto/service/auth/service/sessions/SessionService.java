package com.seccreto.service.auth.service.sessions;

import com.seccreto.service.auth.model.sessions.Session;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de sessão.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 * Baseado na migração V3.
 */
public interface SessionService {
    
    // Operações básicas CRUD
    Session createSession(UUID userId, String refreshTokenHash, String userAgent, InetAddress ipAddress, LocalDateTime expiresAt);
    Session createSession(UUID userId, String token, String ipAddress, String userAgent);
    List<Session> listAllSessions();
    List<Session> listActiveSessions();
    Optional<Session> findSessionById(UUID id);
    List<Session> findSessionsByUserId(UUID userId);
    List<Session> findActiveSessionsByUser(UUID userId);
    Optional<Session> findSessionByRefreshTokenHash(String refreshTokenHash);
    List<Session> findSessionsByUserAgent(String userAgent);
    List<Session> findSessionsByIpAddress(InetAddress ipAddress);
    List<Session> findExpiredSessions();
    List<Session> findValidSessions();
    List<Session> findValidSessionsByUserId(UUID userId);
    Session updateSession(Session session);
    boolean deleteSessionById(UUID id);
    boolean deleteSessionsByUserId(UUID userId);
    boolean deleteExpiredSessions();
    boolean existsSessionById(UUID id);
    boolean existsSessionByRefreshTokenHash(String refreshTokenHash);
    long countSessions();
    long countSessionsByUserId(UUID userId);
    long countValidSessions();
    
    // Operações de gerenciamento de sessão
    void terminateSession(UUID id);
    void invalidateAllUserSessions(UUID userId);
    int cleanupExpiredSessions();
    
    // Métricas e estatísticas
    long countActiveSessions();
    long countExpiredSessions();
    long countSessionsToday();
    long countSessionsThisWeek();
    long countSessionsThisMonth();
    long countSessionsInPeriod(LocalDate startDate, LocalDate endDate);
    long countActiveSessionsInPeriod(LocalDate startDate, LocalDate endDate);
    
    // Operações de busca
    List<Session> searchSessions(String ipAddress, String userAgent, UUID userId);
    String findLastLoginByUser(UUID userId);
    
    // Operações de validação
    boolean isSessionValid(UUID sessionId);
    boolean isSessionExpired(UUID sessionId);
    void refreshSession(UUID sessionId, LocalDateTime newExpiresAt);
}