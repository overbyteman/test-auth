package com.seccreto.service.auth.api.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para requisições de criação de usuário com tenant obrigatório.
 * Conforme solicitado: inclui token de validação de email e associação obrigatória ao tenant.
 */
@Schema(description = "DTO para criação de usuário com tenant obrigatório")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    
    @Schema(description = "Nome do usuário", example = "João Silva")
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;
    
    @Schema(description = "Email do usuário", example = "joao@exemplo.com")
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    private String email;
    
    @Schema(description = "Senha do usuário", example = "MinhaSenh@123")
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
    private String password;
    
    @Schema(description = "ID do tenant (obrigatório)", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "Tenant é obrigatório")
    private UUID tenantId;
    
    @Schema(description = "ID do role inicial (opcional, usa role padrão se não fornecido)", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID initialRoleId;
    
    @Schema(description = "Indica se deve enviar email de validação", example = "true")
    @Builder.Default
    private Boolean sendValidationEmail = true;
    
    @Schema(description = "URL de callback para validação de email", example = "https://app.exemplo.com/validate-email")
    private String validationCallbackUrl;
}
