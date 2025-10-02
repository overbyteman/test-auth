package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para resposta de validação de token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta de validação de token")
public class ValidateTokenResponse {
    
    @Schema(description = "Indica se o token é válido", example = "true")
    private Boolean valid;
    
    @Schema(description = "ID do usuário", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;
    
    @Schema(description = "Nome do usuário", example = "João Silva")
    private String userName;
    
    @Schema(description = "Email do usuário", example = "joao@exemplo.com")
    private String userEmail;
    
    @Schema(description = "Roles do usuário", example = "[\"USER\", \"ADMIN\"]")
    private List<String> roles;
    
    @Schema(description = "Permissões do usuário", example = "[\"READ\", \"WRITE\"]")
    private List<String> permissions;
    
    @Schema(description = "Data de expiração do token")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Motivo da invalidação (se inválido)", example = "Token expirado")
    private String reason;
}
