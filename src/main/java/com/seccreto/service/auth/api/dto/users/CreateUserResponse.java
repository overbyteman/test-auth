package com.seccreto.service.auth.api.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para resposta de criação de usuário.
 * Inclui token de validação de email conforme solicitado.
 */
@Schema(description = "DTO para resposta de criação de usuário")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {
    
    @Schema(description = "ID do usuário criado", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;
    
    @Schema(description = "Nome do usuário", example = "João Silva")
    private String name;
    
    @Schema(description = "Email do usuário", example = "joao@exemplo.com")
    private String email;
    
    @Schema(description = "Data de criação da conta")
    private LocalDateTime createdAt;
    
    @Schema(description = "Status ativo do usuário", example = "false")
    private Boolean isActive;
    
    @Schema(description = "ID do tenant associado", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID tenantId;
    
    @Schema(description = "Nome do tenant associado", example = "Empresa ABC")
    private String tenantName;
    
    @Schema(description = "ID do role inicial atribuído", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID initialRoleId;
    
    @Schema(description = "Nome do role inicial atribuído", example = "USER")
    private String initialRoleName;
    
    @Schema(description = "Token de validação de email", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String emailValidationToken;
    
    @Schema(description = "Data de expiração do token de validação")
    private LocalDateTime tokenExpiresAt;
    
    @Schema(description = "URL completa para validação de email", example = "https://app.exemplo.com/validate-email?token=...")
    private String validationUrl;
    
    @Schema(description = "Indica se o email de validação foi enviado", example = "true")
    private Boolean emailSent;
    
    @Schema(description = "Mensagem informativa sobre próximos passos")
    private String message;
}
