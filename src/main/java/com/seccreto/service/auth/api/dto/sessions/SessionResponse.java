package com.seccreto.service.auth.api.dto.sessions;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para respostas de sessão.
 */
@Schema(description = "DTO para respostas de sessão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    @Schema(description = "Identificador único da sessão", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    
    @Schema(description = "ID do usuário proprietário da sessão", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID userId;
    
    @Schema(description = "User agent do cliente", example = "Mozilla/5.0...")
    private String userAgent;
    
    @Schema(description = "Endereço IP do cliente", example = "192.168.1.1")
    private String ipAddress;
    
    @Schema(description = "Data e hora de expiração da sessão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Data e hora de criação da sessão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Indica se a sessão está válida (não expirada)", example = "true")
    private Boolean isValid;
}
