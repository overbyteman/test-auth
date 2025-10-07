package com.seccreto.service.auth.api.dto.permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "DTO para atualizações parciais de uma permissão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionUpdateRequest {

    @Schema(description = "Nova ação da permissão", example = "update")
    @Size(min = 2, max = 50, message = "Ação deve ter entre 2 e 50 caracteres")
    private String action;

    @Schema(description = "Novo recurso da permissão", example = "members")
    @Size(min = 2, max = 100, message = "Recurso deve ter entre 2 e 100 caracteres")
    private String resource;
}




