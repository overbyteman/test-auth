package com.seccreto.service.auth.model.permissions;

import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.policies.Policy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Classe que representa uma permissão no sistema (JPA Entity)
 *
 * Características de implementação sênior:
 * - JPA Entity com mapeamento automático
 * - Suporte a RBAC (Role-Based Access Control)
 * - Combinação única de action + resource
 * - Cada permission pode ter sua própria policy baseada na role
 * - Relacionamento via pivot RolesPermissions com policies dedicadas
 * - Relacionamento many-to-one com policy
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Entity
@Table(name = "permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"landlord_id", "action", "resource"})
})
@Schema(description = "Entidade que representa uma permissão no sistema para RBAC com policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"rolePermissions", "policy"})
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @Schema(description = "Identificador único da permissão (UUID)")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "action", nullable = false)
    @Schema(description = "Ação da permissão", example = "create")
    private String action;

    @Column(name = "resource", nullable = false)
    @Schema(description = "Recurso da permissão", example = "users")
    private String resource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    @Schema(description = "Landlord proprietário desta permissão")
    private Landlord landlord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    @Schema(description = "Policy associada a esta permission (ABAC)")
    private Policy policy;

    @OneToMany(mappedBy = "permission", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Schema(description = "Associações role-permission que fazem uso desta permission")
    @JsonIgnore
    private Set<RolesPermissions> rolePermissions = new HashSet<>();

    /**
     * Construtor para criação de novas permissões com valores padrão
     */
    public static Permission createNew(String action, String resource, Landlord landlord) {
        return Permission.builder()
                .action(action)
                .resource(resource)
                .landlord(landlord)
                .rolePermissions(new HashSet<>())
                .build();
    }

    /**
     * Construtor para criação de novas permissões com policy
     */
    public static Permission createNew(String action, String resource, Policy policy, Landlord landlord) {
        return Permission.builder()
                .action(action)
                .resource(resource)
                .policy(policy)
                .landlord(landlord)
                .rolePermissions(new HashSet<>())
                .build();
    }
    
    /**
     * Retorna a permissão no formato "action:resource"
     */
    public String getPermissionString() {
        return action + ":" + resource;
    }
    
    /**
     * Verifica se esta permission tem uma policy associada
     */
    public boolean hasPolicy() {
        return this.policy != null;
    }

    /**
     * Associa uma policy a esta permission
     */
    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    /**
     * Retorna os roles que utilizam esta permission através das associações pivot.
     */
    @Transient
    public Set<Role> getRoles() {
        if (this.rolePermissions == null) {
            return Set.of();
        }
        return this.rolePermissions.stream()
                .map(RolesPermissions::getRole)
                .collect(Collectors.toUnmodifiableSet());
    }
}
