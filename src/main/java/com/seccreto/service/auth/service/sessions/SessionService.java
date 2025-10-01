package com.seccreto.service.auth.service.sessions;

import com.seccreto.service.auth.model.sessions.Session;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Abstração da camada de serviço para operações de sessão.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 */
public interface SessionService {
    Session createSession(Long userId, String refreshTokenHash, String userAgent, InetAddress ipAddress, LocalDateTime expiresAt);
    Session createSession(Long userId, String token, String ipAddress, String userAgent);
    List<Session> listAllSessions();
    List<Session> listActiveSessions();
    Optional<Session> findSessionById(Long id);
    List<Session> findSessionsByUserId(Long userId);
    List<Session> findActiveSessionsByUser(Long userId);
    Optional<Session> findSessionByRefreshTokenHash(String refreshTokenHash);
    List<Session> findSessionsByUserAgent(String userAgent);
    List<Session> findSessionsByIpAddress(InetAddress ipAddress);
    List<Session> findExpiredSessions();
    List<Session> findValidSessions();
    List<Session> findValidSessionsByUserId(Long userId);
    Session updateSession(Session session);
    boolean deleteSessionById(Long id);
    boolean deleteSessionsByUserId(Long userId);
    boolean deleteExpiredSessions();
    boolean existsSessionById(Long id);
    boolean existsSessionByRefreshTokenHash(String refreshTokenHash);
    long countSessions();
    long countSessionsByUserId(Long userId);
    long countValidSessions();
    
    // Métodos adicionais para controllers
    void terminateSession(Long id);
    void invalidateAllUserSessions(Long userId);
    Long cleanupExpiredSessions();
    long countActiveSessions();
    long countExpiredSessions();
    long countSessionsToday();
    long countSessionsThisWeek();
    long countSessionsThisMonth();
    long countSessionsInPeriod(String startDate, String endDate);
    long countActiveSessionsInPeriod(String startDate, String endDate);
    List<Session> searchSessions(String ipAddress, String userAgent, Long userId);
    String findLastLoginByUser(Long userId);

    // Métodos adicionais chamados pelos controladores
    long countSessionsByUser(Long userId);
    long countActiveSessionsByUser(Long userId);
}
