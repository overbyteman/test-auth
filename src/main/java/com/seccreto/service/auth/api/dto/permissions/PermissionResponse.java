package com.seccreto.service.auth.api.dto.permissions;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para respostas de permissão.
 */
@Schema(description = "DTO para respostas de permissão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    @Schema(description = "Identificador único da permissão", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    
    @Schema(description = "Ação da permissão", example = "create")
    private String action;

    @Schema(description = "Recurso da permissão", example = "users")
    private String resource;

    @Schema(description = "Identificador do landlord ao qual a permissão pertence", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID landlordId;

    @Schema(description = "Nome do landlord ao qual a permissão pertence", example = "Acme Holdings")
    private String landlordName;
    
    @Schema(description = "Permissão no formato 'action:resource'", example = "create:users")
    private String permissionString;

    @Schema(description = "Identificador da policy associada à permissão", example = "bbbbbbbb-cccc-dddd-eeee-aaaa00000001")
    private UUID policyId;

    @Schema(description = "Código da policy associada", example = "admin-full-access")
    private String policyCode;

    @Schema(description = "Nome da policy associada", example = "Admin Full Access")
    private String policyName;
}
