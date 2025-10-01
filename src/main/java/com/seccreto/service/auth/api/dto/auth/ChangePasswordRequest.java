package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para requisição de alteração de senha.
 */
@Data
@Schema(description = "Dados para alteração de senha")
public class ChangePasswordRequest {
    
    @NotBlank(message = "Senha atual é obrigatória")
    @Schema(description = "Senha atual do usuário", example = "senhaAtual123")
    private String currentPassword;
    
    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "Nova senha deve ter pelo menos 8 caracteres")
    @Schema(description = "Nova senha do usuário", example = "novaSenha123")
    private String newPassword;
}