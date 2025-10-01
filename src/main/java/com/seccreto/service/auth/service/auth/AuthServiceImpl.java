package com.seccreto.service.auth.service.auth;

import com.seccreto.service.auth.api.dto.auth.LoginResponse;
import com.seccreto.service.auth.api.dto.auth.RegisterResponse;
import com.seccreto.service.auth.api.dto.auth.ValidateTokenResponse;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.api.mapper.users.UserMapper;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.sessions.SessionService;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementação do serviço de autenticação.
 */
@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final SessionService sessionService;

    public AuthServiceImpl(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse authenticateUser(String email, String password) {
        validateEmail(email);
        validatePassword(password);
        
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        // TODO: Implementar validação de senha com hash
        // Por enquanto, apenas simula autenticação
        
        // Criar sessão
        String sessionToken = "session_" + System.currentTimeMillis();
        sessionService.createSession(user.getId(), sessionToken, "127.0.0.1", "Mozilla/5.0");
        
        return LoginResponse.builder()
                .accessToken(sessionToken)
                .refreshToken("refresh_" + System.currentTimeMillis())
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .loginTime(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse registerUser(String name, String email, String password) {
        validateName(name);
        validateEmail(email);
        validatePassword(password);
        
        // Verificar se email já existe
        if (userService.existsUserByEmail(email)) {
            throw new ConflictException("Email já cadastrado");
        }
        
        User user = userService.createUser(name, email);
        
        // Criar sessão para o novo usuário
        String sessionToken = "session_" + System.currentTimeMillis();
        sessionService.createSession(user.getId(), sessionToken, "127.0.0.1", "Mozilla/5.0");
        
        return RegisterResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .accessToken(sessionToken)
                .refreshToken("refresh_" + System.currentTimeMillis())
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logoutUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new ValidationException("Token é obrigatório");
        }
        
        // TODO: Implementar invalidação de token
        // Por enquanto, apenas simula logout
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new ValidationException("Refresh token é obrigatório");
        }
        
        // TODO: Implementar renovação de token
        // Por enquanto, apenas simula renovação
        return LoginResponse.builder()
                .accessToken("new_session_" + System.currentTimeMillis())
                .refreshToken("new_refresh_" + System.currentTimeMillis())
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(1L)
                .userName("User")
                .userEmail("user@example.com")
                .loginTime(LocalDateTime.now())
                .build();
    }

    @Override
    public ValidateTokenResponse validateAccessToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new ValidationException("Token é obrigatório");
        }
        
        // TODO: Implementar validação real de token JWT
        // Por enquanto, apenas simula validação
        return ValidateTokenResponse.builder()
                .valid(true)
                .userId(1L)
                .userName("User")
                .userEmail("user@example.com")
                .roles(List.of("USER"))
                .permissions(List.of("READ"))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    @Override
    public UserResponse getCurrentUserProfile(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new ValidationException("Token é obrigatório");
        }
        
        // TODO: Implementar extração de usuário do token
        // Por enquanto, retorna usuário mock
        User user = userService.findUserById(1L)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String token, String currentPassword, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new ValidationException("Token é obrigatório");
        }
        validatePassword(currentPassword);
        validatePassword(newPassword);
        
        // TODO: Implementar alteração de senha
    }

    @Override
    public void sendPasswordRecoveryEmail(String email) {
        validateEmail(email);
        
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email não encontrado"));
        
        // TODO: Implementar envio de email
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new ValidationException("Token é obrigatório");
        }
        validatePassword(newPassword);
        
        // TODO: Implementar redefinição de senha
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome é obrigatório");
        }
        if (name.trim().length() < 2) {
            throw new ValidationException("Nome deve ter pelo menos 2 caracteres");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email é obrigatório");
        }
        if (!email.trim().contains("@")) {
            throw new ValidationException("Email deve ter formato válido");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Senha é obrigatória");
        }
        if (password.trim().length() < 6) {
            throw new ValidationException("Senha deve ter pelo menos 6 caracteres");
        }
    }
}
