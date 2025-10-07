package com.seccreto.service.auth.api.dto.roles_permissions;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para respostas de relacionamento role-permissão.
 */
@Schema(description = "DTO para respostas de relacionamento role-permissão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolesPermissionsResponse {
    @Schema(description = "ID do role", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID roleId;
    
    @Schema(description = "Nome do role", example = "ADMIN")
    private String roleName;

    @Schema(description = "Código do role", example = "admin")
    private String roleCode;
    
    @Schema(description = "ID da permissão", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID permissionId;
    
    @Schema(description = "ID da policy dedicada (se houver)", example = "d2f5f4f9-9e16-4d7c-bb8a-8d9f5a9b1c1f")
    private UUID policyId;

    @Schema(description = "Ação representada pela permissão", example = "read")
    private String permissionAction;

    @Schema(description = "Recurso representado pela permissão", example = "members")
    private String permissionResource;

    @Schema(description = "String combinada da permissão", example = "read:members")
    private String permissionString;

    @Schema(description = "Código da policy associada", example = "reception-access")
    private String policyCode;

    @Schema(description = "Nome da policy associada", example = "Reception Access")
    private String policyName;

    @Schema(description = "Efeito da policy associada", example = "ALLOW")
    private String policyEffect;
}
