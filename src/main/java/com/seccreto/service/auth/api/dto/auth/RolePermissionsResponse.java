package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role do usuário com suas permissões")
public class RolePermissionsResponse {

    @Schema(description = "Nome do role", example = "SUPER_ADMIN")
    private String roleName;

    @Schema(description = "Permissões concedidas pelo role")
    private List<String> permissions;
}
