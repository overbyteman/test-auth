package com.seccreto.service.auth.api.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Payload de criação/atualização de usuário")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @Schema(description = "Nome completo do usuário", example = "João Silva")
    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @Schema(description = "Email do usuário", example = "joao@email.com")
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;
}

