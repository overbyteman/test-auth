package com.seccreto.service.auth.service.auth;

import com.seccreto.service.auth.api.dto.auth.LoginResponse;
import com.seccreto.service.auth.api.dto.auth.RegisterResponse;
import com.seccreto.service.auth.api.dto.auth.ValidateTokenResponse;
import com.seccreto.service.auth.api.dto.users.UserResponse;

/**
 * Interface para operações de autenticação.
 */
public interface AuthService {
    
    /**
     * Autentica um usuário no sistema.
     */
    LoginResponse authenticateUser(String email, String password);
    
    /**
     * Registra um novo usuário no sistema.
     */
    RegisterResponse registerUser(String name, String email, String password);
    
    /**
     * Faz logout de um usuário.
     */
    void logoutUser(String token);
    
    /**
     * Renova o token de acesso.
     */
    LoginResponse refreshAccessToken(String refreshToken);
    
    /**
     * Valida um token de acesso.
     */
    ValidateTokenResponse validateAccessToken(String token);
    
    /**
     * Obtém o perfil do usuário atual.
     */
    UserResponse getCurrentUserProfile(String token);
    
    /**
     * Altera a senha do usuário.
     */
    void changePassword(String token, String currentPassword, String newPassword);
    
    /**
     * Envia email de recuperação de senha.
     */
    void sendPasswordRecoveryEmail(String email);
    
    /**
     * Redefine a senha usando token de recuperação.
     */
    void resetPassword(String token, String newPassword);
}
