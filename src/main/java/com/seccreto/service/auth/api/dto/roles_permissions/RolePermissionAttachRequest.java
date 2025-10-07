package com.seccreto.service.auth.api.dto.roles_permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para anexar uma permissão a um role")
public class RolePermissionAttachRequest {

    @NotNull(message = "ID da permissão é obrigatório")
    @Schema(description = "ID da permissão a ser anexada", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID permissionId;

    @Schema(description = "ID da policy específica para esta associação. Se omitido, pode herdar ou limpar conforme flags", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private UUID policyId;

    @Schema(description = "Quando verdade e nenhuma policy for informada, herdará a policy padrão da permissão", defaultValue = "true")
    private Boolean inheritPermissionPolicy;
}
