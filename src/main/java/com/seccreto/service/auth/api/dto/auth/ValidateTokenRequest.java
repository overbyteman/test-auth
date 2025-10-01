package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para requisição de validação de token.
 */
@Data
@Schema(description = "Dados para validação de token")
public class ValidateTokenRequest {
    
    @NotBlank(message = "Token é obrigatório")
    @Schema(description = "Token de acesso a ser validado", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
}
