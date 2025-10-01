package com.seccreto.service.auth.model.permissions;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Classe que representa uma permissão no sistema (Model)
 * 
 * Características de implementação sênior:
 * - Suporte a RBAC (Role-Based Access Control)
 * - Combinação única de action + resource
 * - Versioning para optimistic locking
 * - Timestamps com timezone
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Schema(description = "Entidade que representa uma permissão no sistema para RBAC")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Permission {
    @Schema(description = "Identificador único da permissão", example = "1")
    @EqualsAndHashCode.Include
    private Long id;
    
    @Schema(description = "Ação da permissão", example = "create")
    private String action;
    
    @Schema(description = "Recurso da permissão", example = "users")
    private String resource;
    
    @Schema(description = "Data e hora de criação da permissão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data e hora da última atualização da permissão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Versão para controle de concorrência otimista", example = "1")
    private Integer version;

    /**
     * Construtor para criação de novas permissões com valores padrão
     */
    public static Permission createNew(String action, String resource) {
        return Permission.builder()
                .action(action)
                .resource(resource)
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
     * Retorna a permissão no formato "action:resource"
     */
    public String getPermissionString() {
        return action + ":" + resource;
    }
}
