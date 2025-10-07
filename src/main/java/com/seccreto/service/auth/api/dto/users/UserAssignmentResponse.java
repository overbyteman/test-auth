package com.seccreto.service.auth.api.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta consolidado para atribuição de roles/permissões.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta padronizada para atribuição de roles e permissões a um usuário")
public class UserAssignmentResponse {

    @Schema(description = "Identificador do usuário", example = "321e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(description = "Identificador do tenant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID tenantId;

    @Schema(description = "Roles informados na requisição")
    private List<UUID> requestedRoleIds;

    @Schema(description = "Roles atribuídos nesta chamada")
    private List<UUID> newlyAssignedRoleIds;

    @Schema(description = "Roles já presentes antes da chamada")
    private List<UUID> alreadyAssignedRoleIds;

    @Schema(description = "Permissões informadas explicitamente na requisição")
    private List<UUID> requestedPermissionIds;

    @Schema(description = "Permissões atribuídas nesta chamada")
    private List<UUID> newlyAssignedPermissionIds;

    @Schema(description = "Permissões que já estavam atribuídas")
    private List<UUID> alreadyAssignedPermissionIds;

    @Schema(description = "Permissões propagadas automaticamente a partir dos roles")
    private List<UUID> propagatedPermissionIds;

    @Schema(description = "Detalhes atuais dos roles do usuário no tenant")
    private List<AssignedRoleDetail> tenantRoles;

    @Schema(description = "Detalhes atuais das permissões diretas do usuário no tenant")
    private List<AssignedPermissionDetail> tenantPermissions;

    @Schema(description = "Quantidade total de roles do usuário no tenant")
    private long totalRolesForUserInTenant;

    @Schema(description = "Quantidade total de permissões diretas do usuário no tenant")
    private long totalDirectPermissionsForUserInTenant;

    @Schema(description = "Mensagem contextual do resultado")
    private String message;
}
