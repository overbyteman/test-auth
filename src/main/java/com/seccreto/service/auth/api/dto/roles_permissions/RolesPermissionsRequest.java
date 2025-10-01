package com.seccreto.service.auth.api.dto.roles_permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para requisições de criação de relacionamento role-permissão.
 */
@Schema(description = "DTO para requisições de relacionamento role-permissão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolesPermissionsRequest {
    @Schema(description = "ID do role", example = "1", required = true)
    @NotNull(message = "ID do role não pode ser nulo")
    @Positive(message = "ID do role deve ser maior que zero")
    private Long roleId;
    
    @Schema(description = "ID da permissão", example = "1", required = true)
    @NotNull(message = "ID da permissão não pode ser nulo")
    @Positive(message = "ID da permissão deve ser maior que zero")
    private Long permissionId;
}
