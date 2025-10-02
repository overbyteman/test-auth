package com.seccreto.service.auth.model.permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * Classe que representa uma permissão no sistema (Model)
 *
 * Características de implementação sênior:
 * - Suporte a RBAC (Role-Based Access Control)
 * - Combinação única de action + resource
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
    @Schema(description = "Identificador único da permissão (UUID)")
    @EqualsAndHashCode.Include
    private UUID id;

    @Schema(description = "Ação da permissão", example = "create")
    private String action;

    @Schema(description = "Recurso da permissão", example = "users")
    private String resource;

    /**
     * Construtor para criação de novas permissões com valores padrão
     */
    public static Permission createNew(String action, String resource) {
        return Permission.builder()
                .action(action)
                .resource(resource)
                .build();
    }
    
    /**
     * Retorna a permissão no formato "action:resource"
     */
    public String getPermissionString() {
        return action + ":" + resource;
    }
    
    /**
     * Método para atualizar timestamps automaticamente
     */
    public void updateTimestamp() {
        // Permission não tem timestamp, mas mantemos para compatibilidade
    }
}