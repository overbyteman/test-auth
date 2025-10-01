package com.seccreto.service.auth.api.dto.users_tenants_roles;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para requisições de criação de relacionamento user-tenant-role.
 */
@Schema(description = "DTO para requisições de relacionamento user-tenant-role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersTenantsRolesRequest {
    @Schema(description = "ID do usuário", example = "1", required = true)
    @NotNull(message = "ID do usuário não pode ser nulo")
    @Positive(message = "ID do usuário deve ser maior que zero")
    private Long userId;
    
    @Schema(description = "ID do tenant", example = "1", required = true)
    @NotNull(message = "ID do tenant não pode ser nulo")
    @Positive(message = "ID do tenant deve ser maior que zero")
    private Long tenantId;
    
    @Schema(description = "ID do role", example = "1", required = true)
    @NotNull(message = "ID do role não pode ser nulo")
    @Positive(message = "ID do role deve ser maior que zero")
    private Long roleId;
}
