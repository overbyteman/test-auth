package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Informações de acesso do usuário a um tenant específico retornadas em respostas de autenticação.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mapa de roles e permissions do usuário por tenant")
public class TenantAccessResponse {

    @Schema(description = "ID do tenant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID tenantId;

    @Schema(description = "Nome do tenant", example = "Tenant Principal")
    private String tenantName;

    @Schema(description = "Roles do usuário neste tenant")
    private List<String> roles;

    @Schema(description = "Permissions do usuário neste tenant")
    private List<String> permissions;
}
