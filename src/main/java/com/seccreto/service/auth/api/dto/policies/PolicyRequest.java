package com.seccreto.service.auth.api.dto.policies;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO para requisições de criação/atualização de policy.
 */
@Schema(description = "DTO para requisições de policy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRequest {
    @Schema(description = "Nome da policy", example = "Admin Full Access", required = true)
    @NotBlank(message = "Nome não pode ser vazio")
    @Size(min = 2, max = 200, message = "Nome deve ter entre 2 e 200 caracteres")
    private String name;
    
    @Schema(description = "Descrição opcional da policy", example = "Permite acesso total para administradores")
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String description;
    
    @Schema(description = "Efeito da policy", example = "ALLOW", required = true)
    @NotNull(message = "Efeito da policy não pode ser nulo")
    private String effect;
    
    @Schema(description = "Lista de ações que a policy se aplica", example = "[\"create\", \"read\", \"update\", \"delete\"]", required = true)
    @NotEmpty(message = "Ações não podem ser vazias")
    private List<String> actions;
    
    @Schema(description = "Lista de recursos que a policy se aplica", example = "[\"users\", \"articles\"]", required = true)
    @NotEmpty(message = "Recursos não podem ser vazios")
    private List<String> resources;
    
    @Schema(description = "Condições ABAC em formato JSON")
    private JsonNode conditions;
}
