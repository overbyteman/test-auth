package com.seccreto.service.auth.api.dto.sessions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO para requisições de criação/atualização de sessão.
 */
@Schema(description = "DTO para requisições de sessão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionRequest {
    @Schema(description = "ID do usuário proprietário da sessão", example = "1")
    @NotNull(message = "ID do usuário não pode ser nulo")
    @Positive(message = "ID do usuário deve ser maior que zero")
    private Long userId;
    
    @Schema(description = "Hash do refresh token")
    @NotBlank(message = "Hash do refresh token não pode ser vazio")
    @Size(min = 32, message = "Hash do refresh token deve ter pelo menos 32 caracteres")
    private String refreshTokenHash;
    
    @Schema(description = "User agent do cliente", example = "Mozilla/5.0...")
    private String userAgent;
    
    @Schema(description = "Endereço IP do cliente", example = "192.168.1.1")
    private String ipAddress;
    
    @Schema(description = "Data e hora de expiração da sessão")
    @NotNull(message = "Data de expiração não pode ser nula")
    private LocalDateTime expiresAt;
}
