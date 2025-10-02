package com.seccreto.service.auth.api.dto.tenants;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para respostas de tenant.
 */
@Schema(description = "DTO para respostas de tenant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    @Schema(description = "Identificador único do tenant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    
    @Schema(description = "Nome do tenant", example = "Empresa ABC")
    private String name;
    
    @Schema(description = "Configuração específica do tenant em formato JSON")
    private JsonNode config;
    
}
