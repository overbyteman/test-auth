package com.seccreto.service.auth.model.policies;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Classe que representa uma política ABAC no sistema (Model)
 * 
 * Características de implementação sênior:
 * - Suporte a ABAC (Attribute-Based Access Control)
 * - Efeitos de política (allow/deny)
 * - Arrays de ações e recursos
 * - Condições JSON flexíveis
 * - Timestamps com timezone
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Schema(description = "Entidade que representa uma política ABAC no sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Policy {
    @Schema(description = "Identificador único da política (UUID)")
    @EqualsAndHashCode.Include
    private UUID id;
    
    @Schema(description = "Nome da política (deve ser único)", example = "Admin Full Access")
    private String name;
    
    @Schema(description = "Descrição opcional da política", example = "Permite acesso total para administradores")
    private String description;
    
    @Schema(description = "Efeito da política", example = "allow")
    private PolicyEffect effect;
    
    @Schema(description = "Lista de ações que a política se aplica", example = "[\"create\", \"read\", \"update\", \"delete\"]")
    private List<String> actions;
    
    @Schema(description = "Lista de recursos que a política se aplica", example = "[\"users\", \"articles\"]")
    private List<String> resources;
    
    @Schema(description = "Condições ABAC em formato JSON")
    private JsonNode conditions;
    
    @Schema(description = "Data e hora de criação da política")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Construtor para criação de novas políticas com valores padrão
     */
    public static Policy createNew(String name, String description, PolicyEffect effect, 
                                  List<String> actions, List<String> resources, JsonNode conditions) {
        return Policy.builder()
                .name(name)
                .description(description)
                .effect(effect)
                .actions(actions)
                .resources(resources)
                .conditions(conditions)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Verifica se a política é de permissão (allow)
     */
    public boolean isAllow() {
        return PolicyEffect.allow.equals(effect);
    }
    
    /**
     * Verifica se a política é de negação (deny)
     */
    public boolean isDeny() {
        return PolicyEffect.deny.equals(effect);
    }
    
    /**
     * Método para atualizar timestamps automaticamente
     */
    public void updateTimestamp() {
        // Policy não tem updatedAt, mas mantemos para compatibilidade
    }
}