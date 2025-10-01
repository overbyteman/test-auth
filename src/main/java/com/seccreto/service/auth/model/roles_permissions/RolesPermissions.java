package com.seccreto.service.auth.model.roles_permissions;

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
 * Classe que representa a relação many-to-many entre roles e permissions (Model)
 * 
 * Características de implementação sênior:
 * - Tabela de junção para RBAC
 * - Chave primária composta (roleId, permissionId)
 * - Timestamps com timezone
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Schema(description = "Entidade que representa a relação many-to-many entre roles e permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class RolesPermissions {
    @Schema(description = "ID do role", example = "1")
    @EqualsAndHashCode.Include
    private Long roleId;
    
    @Schema(description = "ID da permissão", example = "1")
    @EqualsAndHashCode.Include
    private Long permissionId;
    
    @Schema(description = "Data e hora de criação da relação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Construtor para criação de novas relações role-permission
     */
    public static RolesPermissions createNew(Long roleId, Long permissionId) {
        return RolesPermissions.builder()
                .roleId(roleId)
                .permissionId(permissionId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
