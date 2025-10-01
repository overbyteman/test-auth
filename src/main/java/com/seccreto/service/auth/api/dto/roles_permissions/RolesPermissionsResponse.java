package com.seccreto.service.auth.api.dto.roles_permissions;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respostas de relacionamento role-permissão.
 */
@Schema(description = "DTO para respostas de relacionamento role-permissão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolesPermissionsResponse {
    @Schema(description = "ID do role", example = "1")
    private Long roleId;
    
    @Schema(description = "ID da permissão", example = "1")
    private Long permissionId;
    
    @Schema(description = "Data e hora de criação da relação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
