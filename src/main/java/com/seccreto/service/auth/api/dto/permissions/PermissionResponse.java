package com.seccreto.service.auth.api.dto.permissions;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respostas de permissão.
 */
@Schema(description = "DTO para respostas de permissão")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    @Schema(description = "Identificador único da permissão", example = "1")
    private Long id;
    
    @Schema(description = "Ação da permissão", example = "create")
    private String action;
    
    @Schema(description = "Recurso da permissão", example = "users")
    private String resource;
    
    @Schema(description = "Data e hora de criação da permissão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data e hora da última atualização da permissão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Versão para controle de concorrência otimista", example = "1")
    private Integer version;
    
    @Schema(description = "Permissão no formato 'action:resource'", example = "create:users")
    private String permissionString;
}
