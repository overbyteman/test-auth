package com.seccreto.service.auth.service.auth;

import com.seccreto.service.auth.api.dto.auth.LoginResponse;
import com.seccreto.service.auth.api.dto.auth.RegisterResponse;
import com.seccreto.service.auth.api.dto.auth.UserProfileResponse;
import com.seccreto.service.auth.api.dto.auth.ValidateTokenResponse;
import com.seccreto.service.auth.api.dto.auth.RefreshTokenResponse;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.model.sessions.Session;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.repository.users.UserRepository;
import com.seccreto.service.auth.repository.sessions.SessionRepository;
import com.seccreto.service.auth.service.exception.AuthenticationException;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.usage.UsageService;
import com.seccreto.service.auth.service.jwt.JwtService;
import com.seccreto.service.auth.service.audit.AuditService;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
// Removed BCryptPasswordEncoder import - not available in current dependencies
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementação da camada de serviço contendo regras de negócio para autenticação.
 * Aplica SRP e DIP com transações declarativas.
 *
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a UUIDs
 * - Autenticação e autorização
 * - Criptografia de senhas com BCrypt
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final UsageService usageService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRolePermissionService userRolePermissionService;
    private final AuditService auditService;
    private final PasswordResetService passwordResetService;
    
    public AuthServiceImpl(UserRepository userRepository, 
                         SessionRepository sessionRepository,
                         UsageService usageService,
                         JwtService jwtService,
                         PasswordEncoder passwordEncoder,
                         UserRolePermissionService userRolePermissionService,
                         AuditService auditService,
                         PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.usageService = usageService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRolePermissionService = userRolePermissionService;
        this.auditService = auditService;
        this.passwordResetService = passwordResetService;
    }

    @Override
    @Timed(value = "auth.authenticate", description = "Time taken to authenticate user")
    public LoginResponse authenticateUser(String email, String password) {
        validateEmail(email);
        validatePassword(password);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));

        if (!user.isActive()) {
            throw new AuthenticationException("Usuário inativo");
        }

        if (!validatePassword(password, user.getPasswordHash())) {
            auditService.logLogin(user.getId(), false, "Senha incorreta");
            throw new AuthenticationException("Senha incorreta");
        }

        // Create session and return login response
        Session session = createUserSession(user, "web", null);
        
        // Gerar tokens JWT reais com roles e permissions do banco de dados
        List<String> roles = userRolePermissionService.getUserRoles(user.getId());
        List<String> permissions = userRolePermissionService.getUserPermissions(user.getId());
        
        // Se usuário não tem roles, dar role básico
        if (roles.isEmpty()) {
            roles = List.of("USER");
        }
        
        // Se usuário não tem permissions, dar permissions básicas
        if (permissions.isEmpty()) {
            permissions = List.of("read:profile");
        }
        
        String accessToken = jwtService.generateAccessToken(
            user.getId(), session.getId(), null, roles, permissions
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId(), session.getId());
        
        // Log successful login
        auditService.logLogin(user.getId(), true, null);
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .loginTime(LocalDateTime.now())
                .build();
    }

    @Override
    @Timed(value = "auth.authenticate", description = "Time taken to authenticate user with tenant")
    public LoginResponse authenticateUser(String email, String password, UUID tenantId) {
        LoginResponse response = authenticateUser(email, password);
        
        // Verificar se usuário tem acesso ao tenant
        if (!isUserActiveInTenant(response.getUserId(), tenantId)) {
            throw new ValidationException("Usuário não tem acesso ao tenant");
        }
        
        return response;
    }

    @Override
    public boolean validatePassword(String password, String passwordHash) {
        return password != null && passwordHash != null && passwordEncoder.matches(password, passwordHash);
    }

    @Override
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "auth.create_session", description = "Time taken to create user session")
    public Session createUserSession(User user, String userAgent, InetAddress ipAddress) {
        validateUser(user);
        validateUserAgent(userAgent);

        // Criar refresh token
        String refreshToken = generateRefreshToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7); // 7 dias

        Session session = Session.createNew(user.getId(), refreshToken, userAgent, ipAddress, expiresAt);
        return sessionRepository.save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "auth.create_session", description = "Time taken to create user session with tenant")
    public Session createUserSession(User user, String userAgent, InetAddress ipAddress, UUID tenantId) {
        Session session = createUserSession(user, userAgent, ipAddress);
        
        // Associar sessão ao tenant se necessário
        // Implementação específica para multi-tenancy
        
        return session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "auth.refresh_session", description = "Time taken to refresh user session")
    public Session refreshUserSession(String refreshToken, String userAgent, InetAddress ipAddress) {
        validateRefreshToken(refreshToken);

        Session session = sessionRepository.findByRefreshTokenHash(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada"));

        if (session.isExpired()) {
            throw new ValidationException("Sessão expirada");
        }

        // Atualizar expiração
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        return sessionRepository.save(session);
    }

    @Override
    public boolean validateSession(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return false;
        }

        Optional<Session> session = sessionRepository.findByRefreshTokenHash(sessionToken);
        return session.isPresent() && session.get().isValid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateUserSession(String sessionToken) {
        validateSessionToken(sessionToken);
        
        Session session = sessionRepository.findByRefreshTokenHash(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada"));
        
        session.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        sessionRepository.save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateAllUserSessions(UUID userId) {
        validateUserId(userId);
        sessionRepository.deleteByUserId(userId);
    }

    @Override
    public boolean userHasPermission(UUID userId, UUID tenantId, String action, String resource) {
        try {
            // Use the native query method from UserRepository
            Boolean result = userRepository.userHasPermissionInTenant(userId, tenantId, action, resource);
            return result != null && result;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar permissão do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean userHasRole(UUID userId, UUID tenantId, String roleName) {
        try {
            // Get user tenants with roles and check if the role exists
            List<Object[]> userTenants = userRepository.getUserTenantsWithRoles(userId);
            return userTenants.stream()
                    .anyMatch(row -> tenantId.equals(row[0]) && roleName.equals(row[3]));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar role do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean userHasRole(UUID userId, UUID tenantId, UUID roleId) {
        try {
            // Get user tenants with roles and check if the role ID exists
            List<Object[]> userTenants = userRepository.getUserTenantsWithRoles(userId);
            return userTenants.stream()
                    .anyMatch(row -> tenantId.equals(row[0]) && roleId.equals(row[2]));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar role do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getUserPermissions(UUID userId, UUID tenantId) {
        try {
            // Use the native query method from UserRepository
            List<Object[]> permissions = userRepository.getUserPermissionsInTenant(userId, tenantId);
            return permissions.stream()
                    .map(row -> row[1] + ":" + row[2]) // action:resource
                    .sorted()
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter permissões do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getUserRoles(UUID userId, UUID tenantId) {
        try {
            // Get user tenants with roles and extract role names for the specific tenant
            List<Object[]> userTenants = userRepository.getUserTenantsWithRoles(userId);
            return userTenants.stream()
                    .filter(row -> tenantId.equals(row[0]))
                    .map(row -> (String) row[3]) // role_name
                    .sorted()
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter roles do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isUserActive(UUID userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.isPresent() && user.get().isActive();
    }

    @Override
    public boolean isUserActiveInTenant(UUID userId, UUID tenantId) {
        try {
            // Check if user is active and has roles in the tenant
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty() || !user.get().isActive()) {
                return false;
            }
            
            // Check if user has any role in the tenant
            List<Object[]> userTenants = userRepository.getUserTenantsWithRoles(userId);
            return userTenants.stream()
                    .anyMatch(row -> tenantId.equals(row[0]));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar usuário ativo no tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isSessionValid(String sessionToken) {
        return validateSession(sessionToken);
    }

    @Override
    public boolean isSessionExpired(String sessionToken) {
        Optional<Session> session = sessionRepository.findByRefreshTokenHash(sessionToken);
        return session.isPresent() && session.get().isExpired();
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        validateEmail(email);
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<Session> findSessionByToken(String sessionToken) {
        validateSessionToken(sessionToken);
        return sessionRepository.findByRefreshTokenHash(sessionToken);
    }

    @Override
    public List<Session> findUserActiveSessions(UUID userId) {
        validateUserId(userId);
        return sessionRepository.findActiveSessionsByUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredSessions() {
        return usageService.cleanupExpiredSessions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupInactiveUsers() {
        return usageService.cleanupExpiredSessions(); // Using available method
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse registerUser(String name, String email, String password) {
        validateEmail(email);
        validatePassword(password);
        
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Usuário já existe com este email");
        }
        
        // Create new user
        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(hashPassword(password))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Create session
        Session session = createUserSession(savedUser, "web", null);
        
        // Generate JWT tokens
        List<String> roles = List.of("USER"); // TODO: Fetch real user roles
        List<String> permissions = List.of("read:profile"); // TODO: Fetch real permissions
        
        String accessToken = jwtService.generateAccessToken(
            savedUser.getId(), session.getId(), null, roles, permissions
        );
        String refreshToken = jwtService.generateRefreshToken(savedUser.getId(), session.getId());
        
        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .createdAt(savedUser.getCreatedAt())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    @Override
    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new ValidationException("Refresh token é obrigatório");
        }
        
        // Validar refresh token usando JwtService
        var validationResult = jwtService.validateToken(refreshToken);
        
        if (!validationResult.valid()) {
            throw new AuthenticationException("Refresh token inválido ou expirado");
        }
        
        // Verificar se é um refresh token
        var tokenInfo = jwtService.extractTokenInfo(refreshToken);
        if (tokenInfo == null) {
            throw new AuthenticationException("Não foi possível extrair informações do token");
        }
        
        // Buscar usuário
        User user = userRepository.findById(validationResult.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        if (!user.isActive()) {
            throw new AuthenticationException("Usuário inativo");
        }
        
        // Buscar roles e permissions atualizadas
        List<String> roles = userRolePermissionService.getUserRoles(user.getId());
        List<String> permissions = userRolePermissionService.getUserPermissions(user.getId());
        
        // Se usuário não tem roles, dar role básico
        if (roles.isEmpty()) {
            roles = List.of("USER");
        }
        
        // Se usuário não tem permissions, dar permissions básicas
        if (permissions.isEmpty()) {
            permissions = List.of("read:profile");
        }
        
        // Gerar novo access token
        String newAccessToken = jwtService.generateAccessToken(
            user.getId(), validationResult.sessionId(), validationResult.tenantId(), roles, permissions
        );
        
        // Opcionalmente, gerar novo refresh token (rotação de tokens)
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), validationResult.sessionId());
        
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(user.getId())
                .refreshedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public ValidateTokenResponse validateAccessToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return ValidateTokenResponse.builder()
                    .valid(false)
                    .reason("Token não fornecido")
                    .build();
        }
        
        // Remover prefixo "Bearer " se presente
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        
        JwtService.JwtValidationResult result = jwtService.validateToken(cleanToken);
        
        if (!result.valid()) {
            return ValidateTokenResponse.builder()
                    .valid(false)
                    .userId(result.userId())
                    .reason(result.reason())
                    .build();
        }
        
        // Buscar informações do usuário
        User user = userRepository.findById(result.userId())
                .orElse(null);
        
        return ValidateTokenResponse.builder()
                .valid(true)
                .userId(result.userId())
                .userName(user != null ? user.getName() : "Unknown")
                .userEmail(user != null ? user.getEmail() : "unknown@example.com")
                .roles(result.roles())
                .permissions(result.permissions())
                .expiresAt(result.expiresAt())
                .build();
    }

    @Override
    public UserResponse getCurrentUserProfile(String token) {
        ValidateTokenResponse tokenResponse = validateAccessToken(token);
        if (!tokenResponse.getValid()) {
            throw new ValidationException("Token inválido");
        }
        
        User user = userRepository.findById(tokenResponse.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public UserProfileResponse getCurrentUserCompleteProfile(String token) {
        // Remover prefixo "Bearer " se presente
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        
        JwtService.JwtValidationResult result = jwtService.validateToken(cleanToken);
        
        if (!result.valid()) {
            return UserProfileResponse.builder()
                    .valid(false)
                    .userId(result.userId())
                    .sessionId(result.sessionId())
                    .tenantId(result.tenantId())
                    .userInfo(null)
                    .permissions(List.of())
                    .roles(List.of())
                    .policies(List.of())
                    .expiresAt(result.expiresAt())
                    .reason(result.reason())
                    .build();
        }
        
        User user = userRepository.findById(result.userId())
                .orElse(null);
        
        if (user == null) {
            return UserProfileResponse.builder()
                    .valid(false)
                    .userId(result.userId())
                    .sessionId(result.sessionId())
                    .tenantId(result.tenantId())
                    .userInfo(null)
                    .permissions(List.of())
                    .roles(List.of())
                    .policies(List.of())
                    .expiresAt(result.expiresAt())
                    .reason("Usuário não encontrado")
                    .build();
        }
        
        UserProfileResponse.UserInfo userInfo = UserProfileResponse.UserInfo.of(
                user.getName(),
                user.getEmail(),
                user.isActive()
        );
        
        // TODO: Buscar políticas reais do usuário
        List<String> policies = List.of();
        
        return UserProfileResponse.builder()
                .valid(true)
                .userId(user.getId())
                .sessionId(result.sessionId())
                .tenantId(result.tenantId())
                .userInfo(userInfo)
                .permissions(result.permissions())
                .roles(result.roles())
                .policies(policies)
                .expiresAt(result.expiresAt())
                .reason(null)
                .build();
    }

    @Override
    public void changePassword(String token, String currentPassword, String newPassword) {
        ValidateTokenResponse tokenResponse = validateAccessToken(token);
        if (!tokenResponse.getValid()) {
            throw new ValidationException("Token inválido");
        }
        
        User user = userRepository.findById(tokenResponse.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        if (!validatePassword(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("Senha atual incorreta");
        }
        
        validatePassword(newPassword);
        user.setPasswordHash(hashPassword(newPassword));
        user.updateTimestamp();
        userRepository.save(user);
    }

    @Override
    public void sendPasswordRecoveryEmail(String email) {
        validateEmail(email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
        
        if (!user.isActive()) {
            throw new ValidationException("Não é possível redefinir senha para usuário inativo");
        }
        
        // Gerar token seguro de reset
        String resetToken = passwordResetService.generateResetToken(user.getId());
        
        // TODO: Implement email sending logic with the reset token
        // The token should be sent via secure email with expiration time
        logger.info("Password recovery email requested for user ID: {} with token expiry in 15 minutes", user.getId());
        
        // Em um ambiente real, você enviaria um email com um link como:
        // https://yourdomain.com/reset-password?token={resetToken}
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new ValidationException("Token de recuperação é obrigatório");
        }
        
        validatePassword(newPassword);
        
        try {
            // Validar token e obter ID do usuário
            UUID userId = passwordResetService.validateResetToken(token);
            
            // Buscar usuário
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
            
            if (!user.isActive()) {
                throw new ValidationException("Não é possível redefinir senha para usuário inativo");
            }
            
            // Verificar se a nova senha não é igual à atual
            if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
                throw new ValidationException("A nova senha deve ser diferente da senha atual");
            }
            
            // Atualizar senha
            user.setPasswordHash(hashPassword(newPassword));
            user.updateTimestamp();
            userRepository.save(user);
            
            // Invalidar token usado
            passwordResetService.invalidateToken(token);
            
            // Invalidar todas as sessões do usuário por segurança
            sessionRepository.deleteByUserId(userId);
            
            // Log da ação
            auditService.logPasswordReset(userId, true, null);
            logger.info("Password reset completed successfully for user ID: {}", userId);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Password reset failed: {}", e.getMessage());
            throw new ValidationException("Token inválido ou expirado");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logoutUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new ValidationException("Token é obrigatório");
        }
        
        try {
            // Extrair informações do token (mesmo que expirado)
            JwtService.JwtTokenInfo tokenInfo = jwtService.extractTokenInfo(token);
            
            if (tokenInfo != null && tokenInfo.sessionId() != null) {
                // Invalidar sessão específica
                Optional<Session> session = sessionRepository.findById(tokenInfo.sessionId());
                if (session.isPresent()) {
                    sessionRepository.deleteById(tokenInfo.sessionId());
                    logger.info("Session invalidated for user ID: {} during logout", tokenInfo.userId());
                } else {
                    logger.warn("Session not found during logout for session ID: {}", tokenInfo.sessionId());
                }
                
                // Log da ação de logout
                auditService.logLogout(tokenInfo.userId(), true, null);
            } else {
                logger.warn("Could not extract session information from token during logout");
                throw new ValidationException("Token inválido para logout");
            }
            
        } catch (Exception e) {
            logger.warn("Logout failed: {}", e.getMessage());
            throw new ValidationException("Erro durante logout: token pode estar inválido");
        }
    }

    // Métodos de validação privados
    private void validateUser(User user) {
        if (user == null) {
            throw new ValidationException("Usuário não pode ser nulo");
        }
        if (user.getId() == null) {
            throw new ValidationException("ID do usuário não pode ser nulo");
        }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("ID do usuário não pode ser nulo");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email é obrigatório");
        }
        if (!email.contains("@")) {
            throw new ValidationException("Email deve ter formato válido");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Senha é obrigatória");
        }
        
        // Política de senha forte
        if (password.length() < 8) {
            throw new ValidationException("Senha deve ter pelo menos 8 caracteres");
        }
        
        if (password.length() > 128) {
            throw new ValidationException("Senha não pode ter mais de 128 caracteres");
        }
        
        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecialChar = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        if (!hasUpperCase) {
            throw new ValidationException("Senha deve conter pelo menos uma letra maiúscula");
        }
        
        if (!hasLowerCase) {
            throw new ValidationException("Senha deve conter pelo menos uma letra minúscula");
        }
        
        if (!hasDigit) {
            throw new ValidationException("Senha deve conter pelo menos um número");
        }
        
        if (!hasSpecialChar) {
            throw new ValidationException("Senha deve conter pelo menos um caractere especial (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        }
        
        // Verificar se não contém sequências comuns
        String lowerPassword = password.toLowerCase();
        String[] commonSequences = {"123456", "abcdef", "qwerty", "password", "admin", "user"};
        for (String sequence : commonSequences) {
            if (lowerPassword.contains(sequence)) {
                throw new ValidationException("Senha não pode conter sequências comuns como '" + sequence + "'");
            }
        }
        
        // Verificar se não tem caracteres repetidos consecutivos
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) && password.charAt(i) == password.charAt(i + 2)) {
                throw new ValidationException("Senha não pode ter mais de 2 caracteres iguais consecutivos");
            }
        }
    }

    private void validateUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            throw new ValidationException("User Agent é obrigatório");
        }
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new ValidationException("Refresh token é obrigatório");
        }
    }

    private void validateSessionToken(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            throw new ValidationException("Session token é obrigatório");
        }
    }

    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}