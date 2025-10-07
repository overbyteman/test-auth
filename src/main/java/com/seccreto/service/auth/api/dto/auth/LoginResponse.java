package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

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
    
    @Schema(description = "ID do usuário", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;
    
    @Schema(description = "Nome do usuário", example = "João Silva")
    private String userName;
    
    @Schema(description = "Email do usuário", example = "joao@exemplo.com")
    private String userEmail;
    
    @Schema(description = "ID do tenant ativo", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID tenantId;

    @Schema(description = "Nome do tenant ativo", example = "Tenant Principal")
    private String tenantName;

    @Schema(description = "ID do landlord associado ao tenant", example = "11111111-1111-1111-1111-111111111111")
    private UUID landlordId;

    @Schema(description = "Nome do landlord associado ao tenant")
    private String landlordName;

    @Schema(description = "Data e hora do login")
    private LocalDateTime loginTime;
}
