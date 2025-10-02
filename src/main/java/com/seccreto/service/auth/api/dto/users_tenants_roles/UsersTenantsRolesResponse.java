package com.seccreto.service.auth.api.dto.users_tenants_roles;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para respostas de relacionamento user-tenant-role.
 */
@Schema(description = "DTO para respostas de relacionamento user-tenant-role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersTenantsRolesResponse {
    @Schema(description = "ID do usuário", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;
    
    @Schema(description = "ID do tenant", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID tenantId;
    
    @Schema(description = "ID do role", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID roleId;
    
}
