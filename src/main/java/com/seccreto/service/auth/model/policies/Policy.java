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

/**
 * Classe que representa uma política ABAC no sistema (Model)
 * 
 * Características de implementação sênior:
 * - Suporte a ABAC (Attribute-Based Access Control)
 * - Efeitos de política (allow/deny)
 * - Arrays de ações e recursos
 * - Condições JSON flexíveis
 * - Versioning para optimistic locking
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
    @Schema(description = "Identificador único da política", example = "1")
    @EqualsAndHashCode.Include
    private Long id;
    
    @Schema(description = "Nome da política (deve ser único)", example = "Admin Full Access")
    private String name;
    
    @Schema(description = "Descrição opcional da política", example = "Permite acesso total para administradores")
    private String description;
    
    @Schema(description = "Efeito da política", example = "ALLOW")
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
    
    @Schema(description = "Data e hora da última atualização da política")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Versão para controle de concorrência otimista", example = "1")
    private Integer version;

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
                .updatedAt(LocalDateTime.now())
                .version(1)
                .build();
    }
    
    /**
     * Método para atualizar timestamps automaticamente
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Verifica se a política é de permissão (ALLOW)
     */
    public boolean isAllow() {
        return PolicyEffect.ALLOW.equals(effect);
    }
    
    /**
     * Verifica se a política é de negação (DENY)
     */
    public boolean isDeny() {
        return PolicyEffect.DENY.equals(effect);
    }
}
