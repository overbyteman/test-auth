package com.seccreto.service.auth.api.dto.roles;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisições de criação/atualização de role.
 */
@Schema(description = "DTO para requisições de role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {
    @Schema(description = "Nome do role", example = "ADMIN", required = true)
    @NotBlank(message = "Nome não pode ser vazio")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;
    
    @Schema(description = "Descrição opcional do role", example = "Administrador do sistema")
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String description;
}
