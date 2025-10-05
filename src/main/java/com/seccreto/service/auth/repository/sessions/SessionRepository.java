package com.seccreto.service.auth.repository.sessions;

import com.seccreto.service.auth.model.sessions.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para Session - Substitui o JdbcSessionRepository
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    // ========================================
    // QUERIES DERIVADAS AUTOMÁTICAS (JPA)
    // ========================================
    
    List<Session> findByUserId(UUID userId);
    
    List<Session> findByUserAgent(String userAgent);
    
    List<Session> findByIpAddress(InetAddress ipAddress);
    
    List<Session> findByExpiresAtAfter(LocalDateTime dateTime);
    
    List<Session> findByExpiresAtBefore(LocalDateTime dateTime);

    // ========================================
    // QUERIES CUSTOMIZADAS COM @Query
    // ========================================
    
    @Query("SELECT s FROM Session s WHERE s.expiresAt >= :now")
    List<Session> findActiveSessions(@Param("now") LocalDateTime now);
    
    default List<Session> findActiveSessions() {
        return findActiveSessions(LocalDateTime.now());
    }
    
    @Query("SELECT s FROM Session s WHERE s.expiresAt < :now")
    List<Session> findExpiredSessions(@Param("now") LocalDateTime now);
    
    default List<Session> findExpiredSessions() {
        return findExpiredSessions(LocalDateTime.now());
    }
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.expiresAt >= :now")
    long countActiveSessions(@Param("now") LocalDateTime now);
    
    default long countActiveSessions() {
        return countActiveSessions(LocalDateTime.now());
    }
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.expiresAt < :now")
    long countExpiredSessions(@Param("now") LocalDateTime now);
    
    default long countExpiredSessions() {
        return countExpiredSessions(LocalDateTime.now());
    }
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.createdAt >= :startOfDay AND s.createdAt < :endOfDay")
    long countSessionsToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    default long countSessionsToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return countSessionsToday(startOfDay, endOfDay);
    }
    
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
    
    default int deleteExpiredSessions() {
        return deleteExpiredSessions(LocalDateTime.now());
    }
    
    @Query("DELETE FROM Session s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    // ========================================
    // SEARCH METHODS
    // ========================================
    
    @Query("SELECT s FROM Session s WHERE " +
           "(:ipAddress IS NULL OR s.ipAddress = :ipAddress) AND " +
           "(:userAgent IS NULL OR LOWER(s.userAgent) LIKE LOWER(CONCAT('%', :userAgent, '%'))) AND " +
           "(:userId IS NULL OR s.userId = :userId)")
    List<Session> search(@Param("ipAddress") InetAddress ipAddress, 
                        @Param("userAgent") String userAgent, 
                        @Param("userId") UUID userId);

    @Query("SELECT s FROM Session s WHERE s.refreshTokenHash = :refreshTokenHash")
    Optional<Session> findByRefreshTokenHash(@Param("refreshTokenHash") String refreshTokenHash);
    
    @Query("SELECT s FROM Session s WHERE s.userId = :userId AND s.expiresAt >= :now")
    List<Session> findActiveSessionsByUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    default List<Session> findActiveSessionsByUser(UUID userId) {
        return findActiveSessionsByUser(userId, LocalDateTime.now());
    }
    
    @Query("SELECT s FROM Session s WHERE s.expiresAt >= :now")
    List<Session> findValidSessions(@Param("now") LocalDateTime now);
    
    default List<Session> findValidSessions() {
        return findValidSessions(LocalDateTime.now());
    }
    
    @Query("SELECT s FROM Session s WHERE s.userId = :userId AND s.expiresAt >= :now")
    List<Session> findByUserIdAndValid(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    default List<Session> findByUserIdAndValid(UUID userId) {
        return findByUserIdAndValid(userId, LocalDateTime.now());
    }
    
    boolean existsByRefreshTokenHash(String refreshTokenHash);
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.expiresAt >= :now")
    long countValidSessions(@Param("now") LocalDateTime now);
    
    default long countValidSessions() {
        return countValidSessions(LocalDateTime.now());
    }
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.createdAt >= :weekStart")
    long countSessionsThisWeek(@Param("weekStart") LocalDateTime weekStart);
    
    default long countSessionsThisWeek() {
        return countSessionsThisWeek(LocalDateTime.now().minusDays(7));
    }
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.createdAt >= :monthStart")
    long countSessionsThisMonth(@Param("monthStart") LocalDateTime monthStart);
    
    default long countSessionsThisMonth() {
        return countSessionsThisMonth(LocalDateTime.now().minusDays(30));
    }
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.createdAt BETWEEN :start AND :end")
    long countSessionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    default long countSessionsInPeriod(String startDate, String endDate) {
        return countSessionsInPeriod(LocalDateTime.parse(startDate), LocalDateTime.parse(endDate));
    }
    
    @Query("SELECT COUNT(s) FROM Session s WHERE s.expiresAt >= :now AND s.createdAt BETWEEN :start AND :end")
    long countActiveSessionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("now") LocalDateTime now);
    
    default long countActiveSessionsInPeriod(String startDate, String endDate) {
        return countActiveSessionsInPeriod(LocalDateTime.parse(startDate), LocalDateTime.parse(endDate), LocalDateTime.now());
    }
    
    @Query("SELECT s FROM Session s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<Session> findLastLoginByUser(@Param("userId") UUID userId);
}