package com.seccreto.service.auth.api.dto.roles;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respostas de role.
 */
@Schema(description = "DTO para respostas de role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    @Schema(description = "Identificador único do role", example = "1")
    private Long id;
    
    @Schema(description = "Nome do role", example = "ADMIN")
    private String name;
    
    @Schema(description = "Descrição opcional do role", example = "Administrador do sistema")
    private String description;
    
    @Schema(description = "Data e hora de criação do role")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data e hora da última atualização do role")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Versão para controle de concorrência otimista", example = "1")
    private Integer version;
}
