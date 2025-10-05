package com.seccreto.service.auth.api.dto.roles;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO para requisições de criação/atualização de role.
 */
@Schema(description = "DTO para requisições de role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {
    @Schema(description = "Código único do role dentro do landlord", example = "staff")
    @NotBlank(message = "Código não pode ser vazio")
    @Size(min = 2, max = 100, message = "Código deve ter entre 2 e 100 caracteres")
    private String code;

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
