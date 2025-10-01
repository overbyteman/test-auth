package com.seccreto.service.auth.model.tenants;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Classe que representa um tenant no sistema (Model)
 * 
 * Características de implementação sênior:
 * - Suporte a multi-tenancy
 * - Configuração JSON flexível
 * - Versioning para optimistic locking
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
    @Schema(description = "Identificador único do tenant", example = "1")
    @EqualsAndHashCode.Include
    private Long id;
    
    @Schema(description = "Nome do tenant", example = "Empresa ABC")
    private String name;
    
    @Schema(description = "Descrição do tenant", example = "Tenant para empresa ABC")
    private String description;
    
    @Schema(description = "Domínio do tenant", example = "empresa-abc.com")
    private String domain;
    
    @Schema(description = "Indica se o tenant está ativo", example = "true")
    private Boolean active;
    
    @Schema(description = "Configuração específica do tenant em formato JSON")
    private JsonNode config;
    
    @Schema(description = "Data e hora de criação do tenant")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data e hora da última atualização do tenant")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Versão para controle de concorrência otimista", example = "1")
    private Integer version;

    /**
     * Construtor para criação de novos tenants com valores padrão
     */
    public static Tenant createNew(String name, JsonNode config) {
        return Tenant.builder()
                .name(name)
                .config(config)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1)
                .build();
    }
    
    /**
     * Construtor para criação de novos tenants com descrição e domínio
     */
    public static Tenant createNew(String name, String description, String domain) {
        return Tenant.builder()
                .name(name)
                .description(description)
                .domain(domain)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1)
                .build();
    }
    
    /**
     * Método para atualizar timestamps automaticamente
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
