package com.seccreto.service.auth.api.dto.roles_permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para atualizar a policy de uma associação role-permission")
public class RolePermissionPolicyUpdateRequest {

    @Schema(description = "ID da policy a ser aplicada. Se nulo, comportamento depende da flag de herança.")
    private UUID policyId;

    @Schema(description = "Quando verdadeiro, herda a policy padrão configurada na permissão caso policyId seja nulo.", defaultValue = "true")
    private Boolean inheritPermissionPolicy;
}
