package com.seccreto.service.auth.model.policies;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.tenants.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Policy - Política ABAC (Tabela Normalizada)
 *
 * ESTRUTURA NORMALIZADA (3NF):
 * - PK: id (UUID)
 * - UNIQUE: name
 * - Relacionamento 1:N com Permission
 * - Arrays PostgreSQL para actions e resources (normalização relaxada por performance)
 * - JSONB para conditions (dados semi-estruturados)
 *
 * DESIGN ABAC:
 * - Uma policy pode ser compartilhada por múltiplas permissions
 * - Permite lógica de negócio complexa via JSON conditions
 * - Effect: ALLOW ou DENY
 *
 * OTIMIZAÇÕES:
 * - Cache L2 (policies mudam raramente)
 * - Índices: PK, UNIQUE(name), GIN(actions), GIN(resources), GIN(conditions)
 * - Array types do PostgreSQL para melhor performance que tabelas normalized
 */
@Entity
@Table(name = "policies",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"tenant_id", "name"}),
           @UniqueConstraint(columnNames = {"tenant_id", "code"})
       },
       indexes = {
           @Index(name = "idx_policies_name", columnList = "name"),
           @Index(name = "idx_policies_effect", columnList = "effect")
       })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Schema(description = "Policy - Política ABAC do sistema (estrutura normalizada)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"permissions"})
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @Schema(description = "ID único da policy")
    @EqualsAndHashCode.Include
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @Schema(description = "Tenant ao qual esta policy pertence")
    private Tenant tenant;

    @Column(name = "code", nullable = false, length = 120)
    @Schema(description = "Código único da policy dentro do tenant", example = "staff-view-policy")
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    @Schema(description = "Nome da policy", example = "Admin Full Access")
    private String name;
    
    @Column(name = "description", length = 500)
    @Schema(description = "Descrição da policy")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "effect", nullable = false, length = 10)
    @Schema(description = "Efeito: ALLOW ou DENY")
    private PolicyEffect effect;
    
    /**
     * Array PostgreSQL (performance > normalização para listas pequenas)
     * GIN index criado manualmente via migration para busca rápida
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "actions", nullable = false, columnDefinition = "text[]")
    @Schema(description = "Lista de actions", example = "[\"create\", \"read\", \"update\", \"delete\"]")
    private List<String> actions;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "resources", nullable = false, columnDefinition = "text[]")
    @Schema(description = "Lista de resources", example = "[\"users\", \"articles\"]")
    private List<String> resources;
    
    /**
     * JSONB PostgreSQL para dados semi-estruturados
     * GIN index criado manualmente via migration para queries rápidas
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    @Schema(description = "Condições ABAC em JSON")
    private JsonNode conditions;
    
    /**
     * Relacionamento 1:N com Permission
     * Uma policy pode ser usada por várias permissions
     */
    @OneToMany(mappedBy = "policy", fetch = FetchType.LAZY)
    @Builder.Default
    @Schema(description = "Permissions que usam esta policy")
    private Set<Permission> permissions = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Data de criação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // ========================================
    // FACTORY METHODS
    // ========================================

    public static Policy createNew(Tenant tenant, String code, String name, String description, PolicyEffect effect,
                                  List<String> actions, List<String> resources, JsonNode conditions) {
        return Policy.builder()
                .tenant(tenant)
                .code(code)
                .name(name)
                .description(description)
                .effect(effect)
                .actions(actions)
                .resources(resources)
                .conditions(conditions)
                .permissions(new HashSet<>())
                .build();
    }
    
    // ========================================
    // MÉTODOS DE NEGÓCIO (ABAC Evaluation)
    // ========================================

    public boolean allows(String action, String resource) {
        return this.effect == PolicyEffect.ALLOW
            && this.actions.contains(action)
            && this.resources.contains(resource);
    }
    
    public boolean denies(String action, String resource) {
        return this.effect == PolicyEffect.DENY
            && this.actions.contains(action)
            && this.resources.contains(resource);
    }

    public boolean isAllow() {
        return this.effect == PolicyEffect.ALLOW;
    }

    public boolean isDeny() {
        return this.effect == PolicyEffect.DENY;
    }

    public boolean appliesToAction(String action) {
        return this.actions != null && this.actions.contains(action);
    }

    public boolean appliesToResource(String resource) {
        return this.resources != null && this.resources.contains(resource);
    }

    public int getPermissionCount() {
        return this.permissions != null ? this.permissions.size() : 0;
    }
}