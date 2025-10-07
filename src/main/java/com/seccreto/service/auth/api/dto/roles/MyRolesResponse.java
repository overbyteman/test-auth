package com.seccreto.service.auth.api.dto.roles;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Representa as roles e permissões do usuário autenticado associadas a um landlord específico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Roles e permissões do usuário autenticado para um landlord")
public class MyRolesResponse {

    @Schema(description = "ID do landlord", example = "11111111-1111-1111-1111-111111111111")
    private UUID landlordId;

    @Schema(description = "Nome do landlord", example = "Academia Central")
    private String landlordName;

    @Schema(description = "ID do tenant", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID tenantId;

    @Schema(description = "Nome do tenant", example = "Unidade Copacabana")
    private String tenantName;

    @Schema(description = "Roles atribuídas ao usuário", example = "[\"ADMIN\", \"MANAGER\"]")
    private List<String> roles;

    @Schema(description = "Permissões efetivas do usuário", example = "[\"manage:users\", \"read:dashboard\"]")
    private List<String> permissions;
}
