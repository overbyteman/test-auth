package com.seccreto.service.auth.controller.auth;

import com.seccreto.service.auth.api.dto.auth.ChangePasswordRequest;
import com.seccreto.service.auth.api.dto.auth.ForgotPasswordRequest;
import com.seccreto.service.auth.api.dto.auth.LoginRequest;
import com.seccreto.service.auth.api.dto.auth.LoginResponse;
import com.seccreto.service.auth.api.dto.auth.LogoutRequest;
import com.seccreto.service.auth.api.dto.auth.RefreshTokenRequest;
import com.seccreto.service.auth.api.dto.auth.RefreshTokenResponse;
import com.seccreto.service.auth.config.RateLimitingAspect.RateLimit;
import com.seccreto.service.auth.api.dto.auth.RegisterRequest;
import com.seccreto.service.auth.api.dto.auth.RegisterResponse;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.service.auth.AuthService;
import com.seccreto.service.auth.service.sessions.SessionService;
import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.auth.PasswordMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller semântico para operações de autenticação.
 * Endpoints baseados em casos de uso reais de autenticação.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints semânticos para autenticação e autorização")
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final UserService userService;
    private final PasswordMigrationService passwordMigrationService;

    public AuthController(AuthService authService, SessionService sessionService, 
                         UserService userService, PasswordMigrationService passwordMigrationService) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.userService = userService;
        this.passwordMigrationService = passwordMigrationService;
    }

    /**
     * CASO DE USO: Usuário faz login no sistema
     * Endpoint semântico: POST /api/auth/login
     */
    @Operation(
        summary = "Fazer login", 
        description = "Autentica um usuário no sistema e retorna token de acesso"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
        @ApiResponse(responseCode = "400", description = "Dados de login inválidos")
    })
    @PostMapping("/login")
    @RateLimit(requests = 5, windowMinutes = 1, message = "Muitas tentativas de login. Tente novamente em 1 minuto.")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.authenticateUser(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    /**
     * CASO DE USO: Usuário faz logout do sistema
     * Endpoint semântico: POST /api/auth/logout
     */
    @Operation(
        summary = "Fazer logout", 
        description = "Encerra a sessão do usuário e invalida o token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token inválido ou expirado")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logoutUser(request.getToken());
        return ResponseEntity.ok().build();
    }

    /**
     * CASO DE USO: Usuário registra nova conta
     * Endpoint semântico: POST /api/auth/register
     */
    @Operation(
        summary = "Registrar usuário", 
        description = "Cria uma nova conta de usuário no sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso",
                content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email já cadastrado"),
        @ApiResponse(responseCode = "400", description = "Dados de registro inválidos")
    })
    @PostMapping("/register")
    @RateLimit(requests = 3, windowMinutes = 5, message = "Muitos registros. Tente novamente em 5 minutos.")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.registerUser(
            request.getName(), 
            request.getEmail(), 
            request.getPassword()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * CASO DE USO: Usuário renova token de acesso
     * Endpoint semântico: POST /api/auth/refresh-token
     */
    @Operation(
        summary = "Renovar token", 
        description = "Gera um novo token de acesso usando refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token renovado com sucesso",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
    })
    @PostMapping("/refresh-token")
    @RateLimit(requests = 10, windowMinutes = 1, message = "Muitas tentativas de refresh. Tente novamente em 1 minuto.")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }



    /**
     * CASO DE USO: Usuário altera senha
     * Endpoint semântico: PUT /api/auth/change-password
     */
    @Operation(
        summary = "Alterar senha", 
        description = "Altera a senha do usuário autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Token inválido ou expirado"),
        @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou nova senha inválida")
    })
    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "Token de acesso") @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(token, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * CASO DE USO: Usuário solicita recuperação de senha
     * Endpoint semântico: POST /api/auth/forgot-password
     */
    @Operation(
        summary = "Solicitar recuperação de senha", 
        description = "Envia email com link para recuperação de senha"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email de recuperação enviado"),
        @ApiResponse(responseCode = "404", description = "Email não encontrado")
    })
    @PostMapping("/forgot-password")
    @RateLimit(requests = 3, windowMinutes = 15, message = "Muitas solicitações de recuperação de senha. Tente novamente em 15 minutos.")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendPasswordRecoveryEmail(request.getEmail());
        return ResponseEntity.ok().build();
    }



}
