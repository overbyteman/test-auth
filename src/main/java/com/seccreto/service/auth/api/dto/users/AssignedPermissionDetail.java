package com.seccreto.service.auth.api.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO que descreve uma permissão associada diretamente a um usuário em um tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalhe de permissão atribuída a um usuário")
public class AssignedPermissionDetail {

    @Schema(description = "Identificador da permissão", example = "211e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Ação da permissão", example = "manage")
    private String action;

    @Schema(description = "Recurso da permissão", example = "users")
    private String resource;
}
