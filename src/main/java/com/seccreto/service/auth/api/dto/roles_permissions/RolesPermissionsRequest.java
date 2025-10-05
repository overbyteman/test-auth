package com.seccreto.service.auth.api.dto.roles_permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO para requisições de criação de relacionamento role-permissão.
 */
@Schema(description = "DTO para requisições de relacionamento role-permissão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolesPermissionsRequest {
    @Schema(description = "ID do role", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "ID do role não pode ser nulo")
    private UUID roleId;
    
    @Schema(description = "ID da permissão", example = "550e8400-e29b-41d4-a716-446655440001")
    @NotNull(message = "ID da permissão não pode ser nulo")
    private UUID permissionId;

    @Schema(description = "ID da policy dedicada (opcional)", example = "d2f5f4f9-9e16-4d7c-bb8a-8d9f5a9b1c1f")
    private UUID policyId;
}
