package com.seccreto.service.auth.model.tenants;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.roles.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tenant - Organização/Empresa (Tabela Normalizada)
 *
 * ESTRUTURA NORMALIZADA (3NF):
 * - PK: id (UUID)
 * - UNIQUE: name
 * - Relacionamento 1:N com Role (um tenant tem vários roles)
 * - JSONB para config (dados semi-estruturados)
 *
 * MULTI-TENANCY:
 * - Isolamento total de dados entre tenants
 * - Cada tenant tem seus próprios roles
 * - Config personalizável via JSONB
 *
 * OTIMIZAÇÕES:
 * - Cache L2 (tenants mudam raramente)
 * - Índices: PK, UNIQUE(name), GIN(config)
 * - Cascade apropriado para roles (orphanRemoval)
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
@ToString(exclude = {"roles"})
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
     * Relacionamento 1:N com Role
     * Cascade ALL + orphanRemoval: quando tenant é deletado, roles também são
     */
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Schema(description = "Roles pertencentes a este tenant")
    private Set<Role> roles = new HashSet<>();

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

    public static Tenant createNew(String name, JsonNode config) {
        return Tenant.builder()
                .name(name)
                .config(config)
                .roles(new HashSet<>())
                .build();
    }

    // ========================================
    // MÉTODOS DE NEGÓCIO
    // ========================================

    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(role);
        role.setTenant(this);
    }

    public void removeRole(Role role) {
        if (this.roles != null) {
            this.roles.remove(role);
            role.setTenant(null);
        }
    }

    public boolean hasRole(String roleName) {
        if (this.roles == null) return false;
        return this.roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    public int getRoleCount() {
        return this.roles != null ? this.roles.size() : 0;
    }
}