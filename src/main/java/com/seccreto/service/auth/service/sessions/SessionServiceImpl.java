package com.seccreto.service.auth.service.sessions;

import com.seccreto.service.auth.model.sessions.Session;
import com.seccreto.service.auth.repository.sessions.SessionRepository;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.usage.UsageService;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação da camada de serviço contendo regras de negócio para sessões.
 * Aplica SRP e DIP com transações declarativas.
 * Baseado na migração V3.
 *
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a UUIDs
 * - Gerenciamento de sessões com expiração
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final UsageService usageService;

    public SessionServiceImpl(SessionRepository sessionRepository, UsageService usageService) {
        this.sessionRepository = sessionRepository;
        this.usageService = usageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.create", description = "Time taken to create a session")
    public Session createSession(UUID userId, String refreshTokenHash, String userAgent, InetAddress ipAddress, LocalDateTime expiresAt) {
        validateUserId(userId);
        validateRefreshTokenHash(refreshTokenHash);
        validateExpiresAt(expiresAt);

        Session session = Session.createNew(userId, refreshTokenHash, userAgent, ipAddress, expiresAt);
        return sessionRepository.save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.create", description = "Time taken to create a session with IP string")
    public Session createSession(UUID userId, String token, String ipAddress, String userAgent) {
        validateUserId(userId);
        validateRefreshTokenHash(token);

        Session session = Session.createNew(userId, token, ipAddress, userAgent);
        return sessionRepository.save(session);
    }

    @Override
    @Timed(value = "sessions.list", description = "Time taken to list sessions")
    public List<Session> listAllSessions() {
        return sessionRepository.findAll();
    }

    @Override
    @Timed(value = "sessions.list", description = "Time taken to list active sessions")
    public List<Session> listActiveSessions() {
        return sessionRepository.findActiveSessions();
    }

    @Override
    @Timed(value = "sessions.find", description = "Time taken to find session by id")
    public Optional<Session> findSessionById(UUID id) {
        validateId(id);
        return sessionRepository.findById(id);
    }

    @Override
    @Timed(value = "sessions.find", description = "Time taken to find sessions by user")
    public List<Session> findSessionsByUserId(UUID userId) {
        validateUserId(userId);
        return sessionRepository.findByUserId(userId);
    }

    @Override
    @Timed(value = "sessions.find", description = "Time taken to find active sessions by user")
    public List<Session> findActiveSessionsByUser(UUID userId) {
        validateUserId(userId);
        return sessionRepository.findActiveSessionsByUser(userId);
    }

    @Override
    @Timed(value = "sessions.find", description = "Time taken to find session by refresh token")
    public Optional<Session> findSessionByRefreshTokenHash(String refreshTokenHash) {
        validateRefreshTokenHash(refreshTokenHash);
        return sessionRepository.findByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    public List<Session> findSessionsByUserAgent(String userAgent) {
        return sessionRepository.findByUserAgent(userAgent);
    }

    @Override
    public List<Session> findSessionsByIpAddress(InetAddress ipAddress) {
        return sessionRepository.findByIpAddress(ipAddress);
    }

    @Override
    public List<Session> findExpiredSessions() {
        return sessionRepository.findExpiredSessions();
    }

    @Override
    public List<Session> findValidSessions() {
        return sessionRepository.findValidSessions();
    }

    @Override
    public List<Session> findValidSessionsByUserId(UUID userId) {
        validateUserId(userId);
        return sessionRepository.findByUserIdAndValid(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.update", description = "Time taken to update session")
    public Session updateSession(Session session) {
        validateSession(session);
        return sessionRepository.update(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.delete", description = "Time taken to delete session")
    public boolean deleteSessionById(UUID id) {
        validateId(id);
        return sessionRepository.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSessionsByUserId(UUID userId) {
        validateUserId(userId);
        return sessionRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteExpiredSessions() {
        return sessionRepository.deleteExpiredSessions();
    }

    @Override
    public boolean existsSessionById(UUID id) {
        validateId(id);
        return sessionRepository.existsById(id);
    }

    @Override
    public boolean existsSessionByRefreshTokenHash(String refreshTokenHash) {
        validateRefreshTokenHash(refreshTokenHash);
        return sessionRepository.existsByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    @Timed(value = "sessions.count", description = "Time taken to count sessions")
    public long countSessions() {
        return sessionRepository.count();
    }

    @Override
    public long countSessionsByUserId(UUID userId) {
        validateUserId(userId);
        return sessionRepository.countByUserId(userId);
    }

    @Override
    public long countValidSessions() {
        return sessionRepository.countValidSessions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateSession(UUID id) {
        validateId(id);
        
        Session session = findSessionById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada com ID: " + id));
        
        // Marcar como expirada
        session.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        sessionRepository.update(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidateAllUserSessions(UUID userId) {
        validateUserId(userId);
        sessionRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredSessions() {
        return usageService.cleanupExpiredSessions();
    }

    @Override
    public long countActiveSessions() {
        return sessionRepository.countActiveSessions();
    }

    @Override
    public long countExpiredSessions() {
        return sessionRepository.countExpiredSessions();
    }

    @Override
    public long countSessionsToday() {
        return sessionRepository.countSessionsToday();
    }

    @Override
    public long countSessionsThisWeek() {
        return sessionRepository.countSessionsThisWeek();
    }

    @Override
    public long countSessionsThisMonth() {
        return sessionRepository.countSessionsThisMonth();
    }

    @Override
    public long countSessionsInPeriod(LocalDate startDate, LocalDate endDate) {
        return sessionRepository.countSessionsInPeriod(startDate.toString(), endDate.toString());
    }

    @Override
    public long countActiveSessionsInPeriod(LocalDate startDate, LocalDate endDate) {
        return sessionRepository.countActiveSessionsInPeriod(startDate.toString(), endDate.toString());
    }

    @Override
    public List<Session> searchSessions(String ipAddress, String userAgent, UUID userId) {
        return sessionRepository.search(ipAddress, userAgent, userId);
    }

    @Override
    public String findLastLoginByUser(UUID userId) {
        validateUserId(userId);
        return sessionRepository.findLastLoginByUser(userId);
    }

    @Override
    public boolean isSessionValid(UUID sessionId) {
        Optional<Session> session = findSessionById(sessionId);
        return session.isPresent() && session.get().isValid();
    }

    @Override
    public boolean isSessionExpired(UUID sessionId) {
        Optional<Session> session = findSessionById(sessionId);
        return session.isPresent() && session.get().isExpired();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshSession(UUID sessionId, LocalDateTime newExpiresAt) {
        validateId(sessionId);
        validateExpiresAt(newExpiresAt);
        
        Session session = findSessionById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada com ID: " + sessionId));
        
        session.setExpiresAt(newExpiresAt);
        sessionRepository.update(session);
    }

    // Métodos de validação privados
    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("ID da sessão não pode ser nulo");
        }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("ID do usuário não pode ser nulo");
        }
    }

    private void validateRefreshTokenHash(String refreshTokenHash) {
        if (refreshTokenHash == null || refreshTokenHash.trim().isEmpty()) {
            throw new ValidationException("Hash do refresh token é obrigatório");
        }
        if (refreshTokenHash.length() < 32) {
            throw new ValidationException("Hash do refresh token deve ter pelo menos 32 caracteres");
        }
    }

    private void validateExpiresAt(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            throw new ValidationException("Data de expiração não pode ser nula");
        }
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Data de expiração deve ser no futuro");
        }
    }

    private void validateSession(Session session) {
        if (session == null) {
            throw new ValidationException("Sessão não pode ser nula");
        }
        validateId(session.getId());
        validateUserId(session.getUserId());
        validateRefreshTokenHash(session.getRefreshTokenHash());
    }
}