package com.seccreto.service.auth.api.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO para requisições de atribuição de roles a usuários.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para atribuição de roles a um usuário em um tenant")
public class UserRoleAssignmentRequest {

    @Schema(description = "Lista de roles a serem atribuídos", example = "['111e8400-e29b-41d4-a716-446655440000']")
    @NotEmpty(message = "É necessário informar pelo menos um role")
    private List<UUID> roleIds;
}
