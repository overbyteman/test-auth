package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para requisição de logout.
 */
@Data
@Schema(description = "Dados para logout do usuário")
public class LogoutRequest {
    
    @NotBlank(message = "Token é obrigatório")
    @Schema(description = "Token de acesso a ser invalidado", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
}
