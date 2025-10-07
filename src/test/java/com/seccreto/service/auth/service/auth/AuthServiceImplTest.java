package com.seccreto.service.auth.service.auth;

import com.seccreto.service.auth.api.dto.auth.LoginResponse;
import com.seccreto.service.auth.api.dto.auth.RegisterResponse;
import com.seccreto.service.auth.api.dto.auth.ValidateTokenResponse;
import com.seccreto.service.auth.api.dto.roles.MyRolesResponse;
import com.seccreto.service.auth.model.sessions.Session;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.repository.sessions.SessionRepository;
import com.seccreto.service.auth.repository.users.UserRepository;
import com.seccreto.service.auth.service.audit.AuditService;
import com.seccreto.service.auth.service.exception.AuthenticationException;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.jwt.JwtService;
import com.seccreto.service.auth.service.usage.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String STRONG_PASSWORD = "SenhaForte123!";

    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private UsageService usageService;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserRolePermissionService userRolePermissionService;
    @Mock private AuditService auditService;
    @Mock private PasswordResetService passwordResetService;
    @Mock private PasswordMigrationService passwordMigrationService;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private UUID sessionId;
    private UUID tenantId;
    private UUID landlordId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        landlordId = UUID.randomUUID();

        activeUser = User.builder()
                .id(userId)
                .name("John Doe")
                .email("user@example.com")
                .passwordHash("hashed-password")
                .isActive(true)
                .createdAt(LocalDateTime.of(2024, 1, 10, 12, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 10, 12, 0))
                .build();
    }

    @Test
        void authenticateUserShouldReturnTokensWithTenantInfo() {
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));
        when(passwordMigrationService.verifyAndMigratePassword(STRONG_PASSWORD, activeUser.getPasswordHash(), userId))
                .thenReturn(true);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(sessionId);
            session.setCreatedAt(LocalDateTime.now());
            return session;
        });
        when(userRolePermissionService.getUserRoles(userId)).thenReturn(List.of("ADMIN"));
        when(userRolePermissionService.getUserPermissions(userId)).thenReturn(List.of("manage:users"));
        var tenantAccess = List.of(new TenantAccess(
                tenantId,
                "Tenant Principal",
                landlordId,
                "Landlord X",
                List.of(new TenantAccess.TenantRoleAccess("ADMIN", List.of("manage:users")))
        ));
        when(userRolePermissionService.getUserTenantAccess(userId)).thenReturn(tenantAccess);
        when(jwtService.generateAccessToken(eq(userId), eq(sessionId), isNull(), anyList(), anyList(), anyList()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken(userId, sessionId)).thenReturn("refresh-token");

        LoginResponse response = authService.authenticateUser(activeUser.getEmail(), STRONG_PASSWORD);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(response.getTenantName()).isEqualTo("Tenant Principal");
        assertThat(response.getLandlordId()).isEqualTo(landlordId);
        assertThat(response.getLandlordName()).isEqualTo("Landlord X");

        verify(auditService).logLogin(userId, true, null);
        verify(passwordMigrationService).verifyAndMigratePassword(STRONG_PASSWORD, activeUser.getPasswordHash(), userId);
    }

    @Test
    void authenticateUserShouldThrowWhenInactive() {
        activeUser.setIsActive(false);

        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.authenticateUser(activeUser.getEmail(), STRONG_PASSWORD))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Usuário inativo");

        verify(passwordMigrationService, never()).verifyAndMigratePassword(anyString(), anyString(), any());
    }

    @Test
    void authenticateUserShouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticateUser(activeUser.getEmail(), STRONG_PASSWORD))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(activeUser.getEmail());
    }

    @Test
    void registerUserShouldPersistAndReturnTokens() {
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(STRONG_PASSWORD)).thenReturn("encoded-hash");

        User persisted = User.builder()
                .id(userId)
                .name(activeUser.getName())
                .email(activeUser.getEmail())
                .passwordHash("encoded-hash")
                .isActive(true)
                .createdAt(LocalDateTime.of(2024, 1, 15, 9, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 15, 9, 0))
                .build();

        when(userRepository.save(any(User.class))).thenReturn(persisted);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(sessionId);
            session.setCreatedAt(LocalDateTime.now());
            return session;
        });
        when(jwtService.generateAccessToken(eq(userId), eq(sessionId), isNull(), anyList(), anyList(), anyList()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken(userId, sessionId)).thenReturn("refresh-token");

        RegisterResponse response = authService.registerUser(activeUser.getName(), activeUser.getEmail(), STRONG_PASSWORD);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-hash");
    }

    @Test
    void registerUserShouldFailWhenEmailExists() {
        when(userRepository.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.registerUser(activeUser.getName(), activeUser.getEmail(), STRONG_PASSWORD))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Usuário já existe com este email");
    }

    @Test
    void refreshAccessTokenShouldThrowWhenInvalid() {
        when(jwtService.validateToken("refresh-token"))
                .thenReturn(new JwtService.JwtValidationResult(false, null, null, null, List.of(), List.of(), List.of(), null, "Invalid"));

        assertThatThrownBy(() -> authService.refreshAccessToken("refresh-token"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void validateAccessTokenShouldReturnInvalidWhenTokenMissing() {
        ValidateTokenResponse response = authService.validateAccessToken("   ");

        assertThat(response.getValid()).isFalse();
        assertThat(response.getReason()).isEqualTo("Token não fornecido");
    }

    @Test
    void getCurrentUserRolesShouldReturnTenantAccessEntries() {
        UUID otherTenantId = UUID.randomUUID();
        UUID otherLandlordId = UUID.randomUUID();

        JwtService.JwtValidationResult valid = new JwtService.JwtValidationResult(
                true,
                userId,
                sessionId,
                tenantId,
                List.of("ADMIN"),
                List.of("manage:users", "read:dashboard"),
                List.of(
                        new JwtService.TenantAccessClaim(
                                tenantId,
                                "Tenant Principal",
                                landlordId,
                                "Landlord X",
                                List.of("ADMIN"),
                                List.of("manage:users", "read:dashboard")
                        ),
                        new JwtService.TenantAccessClaim(
                                otherTenantId,
                                "Tenant Secundário",
                                otherLandlordId,
                                "Landlord Y",
                                List.of("COACH"),
                                List.of("read:dashboard")
                        )
                ),
                LocalDateTime.now().plusHours(1),
                null
        );
        when(jwtService.validateToken("token")).thenReturn(valid);

        List<MyRolesResponse> responses = authService.getCurrentUserRoles("Bearer token", null);

        assertThat(responses).hasSize(2);
        MyRolesResponse first = responses.get(0);
        assertThat(first.getLandlordId()).isEqualTo(landlordId);
        assertThat(first.getTenantId()).isEqualTo(tenantId);
        assertThat(first.getRoles()).containsExactly("ADMIN");
        assertThat(first.getPermissions()).containsExactly("manage:users", "read:dashboard");

        List<MyRolesResponse> filtered = authService.getCurrentUserRoles("Bearer token", tenantId);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void getCurrentUserRolesShouldFallbackWhenTenantAccessMissing() {
        JwtService.JwtValidationResult valid = new JwtService.JwtValidationResult(
                true,
                userId,
                sessionId,
                tenantId,
                List.of("COACH"),
                List.of("read:dashboard"),
                List.of(),
                LocalDateTime.now().plusHours(1),
                null
        );
        when(jwtService.validateToken("token")).thenReturn(valid);

        List<MyRolesResponse> responses = authService.getCurrentUserRoles("Bearer token", null);

        assertThat(responses).hasSize(1);
        MyRolesResponse first = responses.get(0);
        assertThat(first.getTenantId()).isEqualTo(tenantId);
        assertThat(first.getRoles()).containsExactly("COACH");
        assertThat(first.getPermissions()).containsExactly("read:dashboard");
    }

    @Test
    void getCurrentUserRolesShouldThrowWhenTokenInvalid() {
        JwtService.JwtValidationResult invalid = new JwtService.JwtValidationResult(
                false,
                userId,
                sessionId,
                tenantId,
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now(),
                "expirado"
        );
        when(jwtService.validateToken("token")).thenReturn(invalid);

        assertThatThrownBy(() -> authService.getCurrentUserRoles("Bearer token", null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void getCurrentUserProfileShouldThrowWhenTokenInvalid() {
        JwtService.JwtValidationResult invalid = new JwtService.JwtValidationResult(
                false,
                userId,
                sessionId,
                null,
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now(),
                "expired"
        );
        when(jwtService.validateToken("token")).thenReturn(invalid);

        assertThatThrownBy(() -> authService.getCurrentUserProfile("token"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Token inválido");
    }
}
