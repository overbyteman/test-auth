package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para requisição de renovação de token.
 */
@Data
@Schema(description = "Dados para renovação de token")
public class RefreshTokenRequest {
    
    @NotBlank(message = "Refresh token é obrigatório")
    @Schema(description = "Token de renovação", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
}
