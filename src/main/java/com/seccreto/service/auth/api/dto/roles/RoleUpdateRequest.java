package com.seccreto.service.auth.api.dto.roles;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO para atualizações de role.
 */
@Schema(description = "DTO para atualização de role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {

    @Schema(description = "Identificador do landlord ao qual o role pertence", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "LandlordId é obrigatório")
    private UUID landlordId;

    @Schema(description = "Nome do role", example = "ADMIN")
    @NotBlank(message = "Nome não pode ser vazio")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;

    @Schema(description = "Descrição opcional do role", example = "Administrador do sistema")
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String description;
}
