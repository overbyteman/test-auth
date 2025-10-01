package com.seccreto.service.auth.api.dto.tenants;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respostas de tenant.
 */
@Schema(description = "DTO para respostas de tenant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    @Schema(description = "Identificador único do tenant", example = "1")
    private Long id;
    
    @Schema(description = "Nome do tenant", example = "Empresa ABC")
    private String name;
    
    @Schema(description = "Configuração específica do tenant em formato JSON")
    private JsonNode config;
    
    @Schema(description = "Data e hora de criação do tenant")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data e hora da última atualização do tenant")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Versão para controle de concorrência otimista", example = "1")
    private Integer version;
}
