package com.seccreto.service.auth.api.dto.roles;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para respostas de role.
 */
@Schema(description = "DTO para respostas de role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    @Schema(description = "Identificador único do role", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    
    @Schema(description = "Nome do role", example = "ADMIN")
    private String name;
    
    @Schema(description = "Descrição opcional do role", example = "Administrador do sistema")
    private String description;
}
