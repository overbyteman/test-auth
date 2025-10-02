package com.seccreto.service.auth.model.tenants;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Classe que representa um tenant no sistema (JPA Entity)
 *
 * Características de implementação sênior:
 * - JPA Entity com mapeamento automático
 * - Suporte a multi-tenancy
 * - Configuração JSON flexível com Hibernate
 * - Timestamps automáticos
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Entity
@Table(name = "tenants")
@Schema(description = "Entidade que representa um tenant no sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @Schema(description = "Identificador único do tenant (UUID)")
    @EqualsAndHashCode.Include
    private UUID id;
    
    @Column(name = "name", nullable = false, unique = true)
    @Schema(description = "Nome do tenant", example = "Empresa ABC")
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config")
    @Schema(description = "Configuração do tenant em formato JSON")
    private JsonNode config;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Data e hora de criação do tenant")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data e hora da última atualização do tenant")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Construtor para criação de novos tenants com valores padrão
     * Timestamps são gerenciados automaticamente pelo Hibernate
     */
    public static Tenant createNew(String name, JsonNode config) {
        return Tenant.builder()
                .name(name)
                .config(config)
                .build();
    }

    /**
     * Método para atualizar timestamps automaticamente
     * Hibernate gerencia automaticamente via @UpdateTimestamp
     */
    public void updateTimestamp() {
        // Hibernate atualiza automaticamente o updatedAt
    }
}