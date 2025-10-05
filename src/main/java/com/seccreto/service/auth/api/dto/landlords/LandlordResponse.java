package com.seccreto.service.auth.api.dto.landlords;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para Landlord
 */
@Data
@Builder
@Schema(description = "Resposta de Landlord")
public class LandlordResponse {
    
    @Schema(description = "ID único do landlord")
    private UUID id;
    
    @Schema(description = "Nome do landlord")
    private String name;
    
    @Schema(description = "Configurações do landlord")
    private JsonNode config;
    
    @Schema(description = "Data de criação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data de atualização")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Número de tenants")
    private int tenantsCount;
    
    @Schema(description = "Número de roles")
    private int rolesCount;
}