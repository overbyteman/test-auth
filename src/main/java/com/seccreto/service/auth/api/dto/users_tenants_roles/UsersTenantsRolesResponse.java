package com.seccreto.service.auth.api.dto.users_tenants_roles;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respostas de relacionamento user-tenant-role.
 */
@Schema(description = "DTO para respostas de relacionamento user-tenant-role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersTenantsRolesResponse {
    @Schema(description = "ID do usuário", example = "1")
    private Long userId;
    
    @Schema(description = "ID do tenant", example = "1")
    private Long tenantId;
    
    @Schema(description = "ID do role", example = "1")
    private Long roleId;
    
    @Schema(description = "Data e hora de criação da relação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
