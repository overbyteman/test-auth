package com.seccreto.service.auth.model.tenants;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.landlords.Landlord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tenant - Organização/Empresa (Tabela Normalizada)
 *
 * ESTRUTURA NORMALIZADA (3NF):
 * - PK: id (UUID)
 * - UNIQUE: name
 * - JSONB para config (dados semi-estruturados)

 * MULTI-TENANCY:
 * - Isolamento total de dados entre tenants
 * - Config personalizável via JSONB
 *
 * OTIMIZAÇÕES:
 * - Cache L2 (tenants mudam raramente)
 * - Índices: PK, UNIQUE(name), GIN(config)
 */
@Entity
@Table(name = "tenants",
       indexes = {
           @Index(name = "idx_tenants_name", columnList = "name"),
           @Index(name = "idx_tenants_created_at", columnList = "created_at")
       })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Schema(description = "Tenant - Organização no sistema multi-tenant (estrutura normalizada)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @Schema(description = "ID único do tenant")
    @EqualsAndHashCode.Include
    private UUID id;
    
    @Column(name = "name", nullable = false, unique = true, length = 200)
    @Schema(description = "Nome único do tenant", example = "Empresa ABC")
    private String name;

    /**
     * JSONB PostgreSQL para configurações flexíveis
     * GIN index criado manualmente via migration
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    @Schema(description = "Configurações personalizadas em JSON")
    private JsonNode config;

    /**
     * Referência ao landlord (matriz) que controla este tenant (filial)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    @Schema(description = "Landlord proprietário deste tenant")
    private Landlord landlord;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Data de criação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data de atualização")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // ========================================
    // FACTORY METHODS
    // ========================================

    public static Tenant createNew(String name, JsonNode config, Landlord landlord) {
        return Tenant.builder()
                .name(name)
                .config(config)
                .landlord(landlord)
                .build();
    }

    // ========================================
    // MÉTODOS DE NEGÓCIO
    // ========================================

    public void setLandlord(Landlord landlord) {
        if (this.landlord != null && this.landlord != landlord) {
            this.landlord.getTenants().remove(this);
        }
        this.landlord = landlord;
        if (landlord != null && !landlord.getTenants().contains(this)) {
            landlord.getTenants().add(this);
        }
    }
}