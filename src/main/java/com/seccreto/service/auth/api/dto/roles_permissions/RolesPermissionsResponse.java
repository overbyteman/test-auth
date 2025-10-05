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
    
    @Schema(description = "ID da permissão", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID permissionId;
    
    @Schema(description = "ID da policy dedicada (se houver)", example = "d2f5f4f9-9e16-4d7c-bb8a-8d9f5a9b1c1f")
    private UUID policyId;
}
