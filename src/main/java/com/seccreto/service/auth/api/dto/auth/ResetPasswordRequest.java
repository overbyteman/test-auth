package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para requisição de redefinição de senha.
 */
@Data
@Schema(description = "Dados para redefinição de senha")
public class ResetPasswordRequest {
    
    @NotBlank(message = "Token é obrigatório")
    @Schema(description = "Token de recuperação de senha", example = "abc123def456")
    private String token;
    
    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "Nova senha deve ter pelo menos 8 caracteres")
    @Schema(description = "Nova senha do usuário", example = "novaSenha123")
    private String newPassword;
}