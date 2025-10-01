package com.seccreto.service.auth.api.dto.permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisições de criação/atualização de permissão.
 */
@Schema(description = "DTO para requisições de permissão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {
    @Schema(description = "Nome da permissão", example = "Criar Usuários", required = true)
    @NotBlank(message = "Nome não pode ser vazio")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;

    @Schema(description = "Descrição da permissão", example = "Permite criar novos usuários no sistema")
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String description;

    @Schema(description = "Ação da permissão", example = "create", required = true)
    @NotBlank(message = "Ação não pode ser vazia")
    @Size(min = 2, max = 50, message = "Ação deve ter entre 2 e 50 caracteres")
    private String action;
    
    @Schema(description = "Recurso da permissão", example = "users", required = true)
    @NotBlank(message = "Recurso não pode ser vazio")
    @Size(min = 2, max = 100, message = "Recurso deve ter entre 2 e 100 caracteres")
    private String resource;
}
