package com.seccreto.service.auth.model.roles_permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

/**
 * Classe que representa a relação many-to-many entre roles e permissions (Model)
 * 
 * Características de implementação sênior:
 * - Tabela de junção para RBAC
 * - Chave primária composta (roleId, permissionId)
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
    @Schema(description = "ID do role (UUID)")
    @EqualsAndHashCode.Include
    private UUID roleId;
    
    @Schema(description = "ID da permissão (UUID)")
    @EqualsAndHashCode.Include
    private UUID permissionId;

    /**
     * Construtor para criação de novas relações role-permission
     */
    public static RolesPermissions createNew(UUID roleId, UUID permissionId) {
        return RolesPermissions.builder()
                .roleId(roleId)
                .permissionId(permissionId)
                .build();
    }
}