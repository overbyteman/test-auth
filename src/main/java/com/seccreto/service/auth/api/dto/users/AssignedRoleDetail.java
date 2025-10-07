package com.seccreto.service.auth.api.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO que descreve um role associado a um usuário em um tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalhe de role atribuído a um usuário")
public class AssignedRoleDetail {

    @Schema(description = "Identificador do role", example = "111e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Nome do role", example = "ADMIN")
    private String name;

    @Schema(description = "Descrição do role")
    private String description;
}
