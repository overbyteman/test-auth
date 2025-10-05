package com.seccreto.service.auth.api.dto.tenants;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisições de criação/atualização de tenant.
 */
@Schema(description = "DTO para requisições de tenant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRequest {
    @Schema(description = "Nome do tenant", example = "Empresa ABC")
    @NotBlank(message = "Nome não pode ser vazio")
    @Size(min = 2, max = 200, message = "Nome deve ter entre 2 e 200 caracteres")
    private String name;
    
    @Schema(description = "Descrição do tenant", example = "Tenant para empresa ABC")
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String description;
    
    @Schema(description = "Domínio do tenant", example = "empresa-abc.com")
    @Size(max = 255, message = "Domínio deve ter no máximo 255 caracteres")
    private String domain;
    
    @Schema(description = "Configuração específica do tenant em formato JSON")
    private JsonNode config;

    @Schema(description = "Identificador do landlord proprietário", example = "11111111-1111-1111-1111-111111111111")
    @NotNull(message = "Landlord é obrigatório")
    private UUID landlordId;
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }

    public UUID getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(UUID landlordId) {
        this.landlordId = landlordId;
    }
}
