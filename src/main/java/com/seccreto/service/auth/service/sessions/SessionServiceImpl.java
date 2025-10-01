package com.seccreto.service.auth.service.sessions;

import com.seccreto.service.auth.model.sessions.Session;
import com.seccreto.service.auth.repository.sessions.SessionRepository;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementação da camada de serviço contendo regras de negócio para sessões.
 * Aplica SRP e DIP com transações declarativas.
 *
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Gerenciamento de sessões e refresh tokens
 * - Limpeza automática de sessões expiradas
 */
@Service
@Transactional(readOnly = true)
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    public SessionServiceImpl(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.create", description = "Time taken to create a session")
    public Session createSession(Long userId, String refreshTokenHash, String userAgent, InetAddress ipAddress, LocalDateTime expiresAt) {
        validateUserId(userId);
        validateRefreshTokenHash(refreshTokenHash);
        validateExpiresAt(expiresAt);

        // Verificar se já existe uma sessão com este refresh token (idempotência)
        Optional<Session> existingSession = sessionRepository.findByRefreshTokenHash(refreshTokenHash);
        if (existingSession.isPresent()) {
            return existingSession.get(); // Retorna a sessão existente (idempotência)
        }

        Session session = Session.createNew(userId, refreshTokenHash, userAgent, ipAddress, expiresAt);
        return sessionRepository.save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.create", description = "Time taken to create a session")
    public Session createSession(Long userId, String token, String ipAddress, String userAgent) {
        validateUserId(userId);
        if (token == null || token.trim().isEmpty()) {
            throw new ValidationException("Token não pode ser vazio");
        }
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new ValidationException("Endereço IP não pode ser vazio");
        }

        // Definir expiração padrão para 7 dias
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            return createSession(userId, token, userAgent, inetAddress, expiresAt);
        } catch (Exception e) {
            throw new ValidationException("Endereço IP inválido: " + ipAddress);
        }
    }

    @Override
    public List<Session> listAllSessions() {
        return sessionRepository.findAll();
    }

    @Override
    public List<Session> listActiveSessions() {
        return sessionRepository.findActiveSessions();
    }

    @Override
    public Optional<Session> findSessionById(Long id) {
        validateId(id);
        return sessionRepository.findById(id);
    }

    @Override
    public List<Session> findSessionsByUserId(Long userId) {
        validateUserId(userId);
        return sessionRepository.findByUserId(userId);
    }

    @Override
    public List<Session> findActiveSessionsByUser(Long userId) {
        validateUserId(userId);
        return sessionRepository.findActiveSessionsByUser(userId);
    }

    @Override
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
        validateIpAddress(ipAddress);
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
    public List<Session> findValidSessionsByUserId(Long userId) {
        validateUserId(userId);
        return sessionRepository.findByUserIdAndValid(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.update", description = "Time taken to update a session")
    public Session updateSession(Session session) {
        validateSession(session);

        sessionRepository.findById(session.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada com ID: " + session.getId()));

        return sessionRepository.update(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.delete", description = "Time taken to delete a session")
    public boolean deleteSessionById(Long id) {
        validateId(id);

        // Verificar se a sessão existe antes de tentar deletar (idempotência)
        if (!sessionRepository.existsById(id)) {
            return false; // Sessão já não existe (idempotência)
        }

        return sessionRepository.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.deleteByUser", description = "Time taken to delete sessions by user")
    public boolean deleteSessionsByUserId(Long userId) {
        validateUserId(userId);
        return sessionRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.deleteExpired", description = "Time taken to delete expired sessions")
    public boolean deleteExpiredSessions() {
        return sessionRepository.deleteExpiredSessions();
    }

    @Override
    public boolean existsSessionById(Long id) {
        validateId(id);
        return sessionRepository.existsById(id);
    }

    @Override
    public boolean existsSessionByRefreshTokenHash(String refreshTokenHash) {
        validateRefreshTokenHash(refreshTokenHash);
        return sessionRepository.existsByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    public long countSessions() {
        return sessionRepository.count();
    }

    @Override
    public long countSessionsByUserId(Long userId) {
        validateUserId(userId);
        return sessionRepository.countByUserId(userId);
    }

    @Override
    public long countValidSessions() {
        return sessionRepository.countValidSessions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.terminate", description = "Time taken to terminate a session")
    public void terminateSession(Long id) {
        validateId(id);

        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada com ID: " + id));

        session.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Força a expiração
        sessionRepository.update(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.invalidateAll", description = "Time taken to invalidate all user sessions")
    public void invalidateAllUserSessions(Long userId) {
        validateUserId(userId);
        sessionRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "sessions.cleanup", description = "Time taken to cleanup expired sessions")
    public Long cleanupExpiredSessions() {
        List<Session> expiredSessions = sessionRepository.findExpiredSessions();
        long count = expiredSessions.size();
        sessionRepository.deleteExpiredSessions();
        return count;
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
    public long countSessionsInPeriod(String startDate, String endDate) {
        if (startDate == null || startDate.trim().isEmpty()) {
            throw new ValidationException("Data de início não pode ser vazia");
        }
        if (endDate == null || endDate.trim().isEmpty()) {
            throw new ValidationException("Data de fim não pode ser vazia");
        }
        return sessionRepository.countSessionsInPeriod(startDate, endDate);
    }

    @Override
    public long countActiveSessionsInPeriod(String startDate, String endDate) {
        if (startDate == null || startDate.trim().isEmpty()) {
            throw new ValidationException("Data de início não pode ser vazia");
        }
        if (endDate == null || endDate.trim().isEmpty()) {
            throw new ValidationException("Data de fim não pode ser vazia");
        }
        return sessionRepository.countActiveSessionsInPeriod(startDate, endDate);
    }

    @Override
    public List<Session> searchSessions(String ipAddress, String userAgent, Long userId) {
        return sessionRepository.search(ipAddress, userAgent, userId);
    }

    @Override
    public String findLastLoginByUser(Long userId) {
        validateUserId(userId);
        return sessionRepository.findLastLoginByUser(userId);
    }

    @Override
    public long countSessionsByUser(Long userId) {
        validateUserId(userId);
        return sessionRepository.countByUserId(userId);
    }

    @Override
    public long countActiveSessionsByUser(Long userId) {
        validateUserId(userId);
        return sessionRepository.findActiveSessionsByUser(userId).size();
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new ValidationException("ID do usuário não pode ser nulo");
        }
        if (userId <= 0) {
            throw new ValidationException("ID do usuário deve ser maior que zero");
        }
    }

    private void validateRefreshTokenHash(String refreshTokenHash) {
        if (refreshTokenHash == null || refreshTokenHash.trim().isEmpty()) {
            throw new ValidationException("Hash do refresh token não pode ser vazio");
        }
        if (refreshTokenHash.trim().length() < 32) {
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

    private void validateIpAddress(InetAddress ipAddress) {
        if (ipAddress == null) {
            throw new ValidationException("Endereço IP não pode ser nulo");
        }
    }

    private void validateId(Long id) {
        if (id == null) {
            throw new ValidationException("ID não pode ser nulo");
        }
        if (id <= 0) {
            throw new ValidationException("ID deve ser maior que zero");
        }
    }

    private void validateSession(Session session) {
        if (session == null) {
            throw new ValidationException("Sessão não pode ser nula");
        }
        validateId(session.getId());
        validateUserId(session.getUserId());
        validateRefreshTokenHash(session.getRefreshTokenHash());
        validateExpiresAt(session.getExpiresAt());
    }
}
