package com.seccreto.service.auth.api.dto.users_tenants_roles;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO para requisições de criação de relacionamento user-tenant-role.
 */
@Schema(description = "DTO para requisições de relacionamento user-tenant-role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersTenantsRolesRequest {
    @Schema(description = "ID do usuário", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    @NotNull(message = "ID do usuário não pode ser nulo")
    private UUID userId;
    
    @Schema(description = "ID do tenant", example = "550e8400-e29b-41d4-a716-446655440001", required = true)
    @NotNull(message = "ID do tenant não pode ser nulo")
    private UUID tenantId;
    
    @Schema(description = "ID do role", example = "550e8400-e29b-41d4-a716-446655440002", required = true)
    @NotNull(message = "ID do role não pode ser nulo")
    private UUID roleId;
}
