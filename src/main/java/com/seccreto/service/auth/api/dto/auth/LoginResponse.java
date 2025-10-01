package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para resposta de login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta de login com tokens de acesso")
public class LoginResponse {
    
    @Schema(description = "Token de acesso JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @Schema(description = "Token de renovação", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
    
    @Schema(description = "Tipo do token", example = "Bearer")
    private String tokenType;
    
    @Schema(description = "Tempo de expiração em segundos", example = "3600")
    private Long expiresIn;
    
    @Schema(description = "ID do usuário", example = "123")
    private Long userId;
    
    @Schema(description = "Nome do usuário", example = "João Silva")
    private String userName;
    
    @Schema(description = "Email do usuário", example = "joao@exemplo.com")
    private String userEmail;
    
    @Schema(description = "Data e hora do login")
    private LocalDateTime loginTime;
}
