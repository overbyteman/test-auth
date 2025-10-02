package com.seccreto.service.auth.service.auth;

import com.seccreto.service.auth.api.dto.auth.LoginResponse;
import com.seccreto.service.auth.api.dto.auth.RegisterResponse;
import com.seccreto.service.auth.api.dto.auth.ValidateTokenResponse;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.model.sessions.Session;
import com.seccreto.service.auth.model.users.User;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de autenticação.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 */
public interface AuthService {
    
    // Operações de autenticação
    LoginResponse authenticateUser(String email, String password);
    LoginResponse authenticateUser(String email, String password, UUID tenantId);
    RegisterResponse registerUser(String name, String email, String password);
    LoginResponse refreshAccessToken(String refreshToken);
    ValidateTokenResponse validateAccessToken(String token);
    UserResponse getCurrentUserProfile(String token);
    void changePassword(String token, String currentPassword, String newPassword);
    void sendPasswordRecoveryEmail(String email);
    void resetPassword(String token, String newPassword);
    void logoutUser(String token);
    boolean validatePassword(String password, String passwordHash);
    String hashPassword(String password);
    
    // Operações de sessão
    Session createUserSession(User user, String userAgent, InetAddress ipAddress);
    Session createUserSession(User user, String userAgent, InetAddress ipAddress, UUID tenantId);
    Session refreshUserSession(String refreshToken, String userAgent, InetAddress ipAddress);
    boolean validateSession(String sessionToken);
    void terminateUserSession(String sessionToken);
    void terminateAllUserSessions(UUID userId);
    
    // Operações de autorização
    boolean userHasPermission(UUID userId, UUID tenantId, String action, String resource);
    boolean userHasRole(UUID userId, UUID tenantId, String roleName);
    boolean userHasRole(UUID userId, UUID tenantId, UUID roleId);
    List<String> getUserPermissions(UUID userId, UUID tenantId);
    List<String> getUserRoles(UUID userId, UUID tenantId);
    
    // Operações de validação
    boolean isUserActive(UUID userId);
    boolean isUserActiveInTenant(UUID userId, UUID tenantId);
    boolean isSessionValid(String sessionToken);
    boolean isSessionExpired(String sessionToken);
    
    // Operações de busca
    Optional<User> findUserByEmail(String email);
    Optional<Session> findSessionByToken(String sessionToken);
    List<Session> findUserActiveSessions(UUID userId);
    
    // Operações de limpeza
    int cleanupExpiredSessions();
    int cleanupInactiveUsers();
}