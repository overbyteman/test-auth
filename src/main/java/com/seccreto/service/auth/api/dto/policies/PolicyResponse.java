package com.seccreto.service.auth.api.dto.policies;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/**
 * DTO para respostas de policy.
 */
@Schema(description = "DTO para respostas de policy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {
    @Schema(description = "Identificador único da policy", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Identificador do tenant ao qual a policy pertence", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID tenantId;

    @Schema(description = "Código único da policy dentro do tenant", example = "staff-view-policy")
    private String code;
    
    @Schema(description = "Nome da policy", example = "Admin Full Access")
    private String name;
    
    @Schema(description = "Descrição opcional da policy", example = "Permite acesso total para administradores")
    private String description;
    
    @Schema(description = "Efeito da policy", example = "ALLOW")
    private String effect;
    
    @Schema(description = "Lista de ações que a policy se aplica", example = "[\"create\", \"read\", \"update\", \"delete\"]")
    private List<String> actions;
    
    @Schema(description = "Lista de recursos que a policy se aplica", example = "[\"users\", \"articles\"]")
    private List<String> resources;
    
    @Schema(description = "Condições ABAC em formato JSON")
    private JsonNode conditions;
    
    @Schema(description = "Data e hora de criação da policy")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    
    @Schema(description = "Indica se a policy é de permissão (ALLOW)", example = "true")
    private Boolean isAllow;
    
    @Schema(description = "Indica se a policy é de negação (DENY)", example = "false")
    private Boolean isDeny;
}
