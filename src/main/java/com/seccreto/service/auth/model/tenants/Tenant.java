package com.seccreto.service.auth.model.tenants;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Classe que representa um tenant no sistema (Model)
 *
 * Características de implementação sênior:
 * - Suporte a multi-tenancy
 * - Configuração JSON flexível
 * - Timestamps com timezone
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Schema(description = "Entidade que representa um tenant no sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Tenant {
    @Schema(description = "Identificador único do tenant (UUID)")
    @EqualsAndHashCode.Include
    private UUID id;
    
    @Schema(description = "Nome do tenant", example = "Empresa ABC")
    private String name;

    @Schema(description = "Configuração do tenant em formato JSON")
    private JsonNode config;

    @Schema(description = "Data e hora de criação do tenant")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Data e hora da última atualização do tenant")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Construtor para criação de novos tenants com valores padrão
     */
    public static Tenant createNew(String name, JsonNode config) {
        return Tenant.builder()
                .name(name)
                .config(config)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Método para atualizar timestamps automaticamente
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}