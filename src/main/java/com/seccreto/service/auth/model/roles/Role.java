package com.seccreto.service.auth.model.roles;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.model.landlords.Landlord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Classe que representa um role no sistema para RBAC (JPA Entity)
 *
 * Características de implementação sênior:
 * - JPA Entity com mapeamento automático
 * - Suporte a RBAC (Role-Based Access Control)
 * - Multi-tenancy: cada role pertence a um landlord (matriz)
 * - Relacionamento com permissions via pivot dedicado
 * - Nome único por landlord
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "landlord_id"}),
    @UniqueConstraint(columnNames = {"code", "landlord_id"})
})
@Schema(description = "Entidade que representa um role no sistema para RBAC com multi-tenancy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"landlord", "rolePermissions"})
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @Schema(description = "Identificador único do role (UUID)")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "code", nullable = false, length = 100)
    @Schema(description = "Identificador de referência único do role por landlord", example = "staff")
    @EqualsAndHashCode.Include
    private String code;

    @Column(name = "name", nullable = false)
    @Schema(description = "Nome do role (deve ser único por landlord)", example = "ADMIN")
    private String name;
    
    @Column(name = "description")
    @Schema(description = "Descrição opcional do role", example = "Administrador do sistema")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    @Schema(description = "Landlord ao qual este role pertence")
    private Landlord landlord;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Schema(description = "Associações role-permission com policies dedicadas")
    @JsonIgnore
    private Set<RolesPermissions> rolePermissions = new HashSet<>();

    /**
     * Construtor para criação de novos roles com valores padrão
     */
    public static Role createNew(String code, String name, String description, Landlord landlord) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code do role é obrigatório");
        }
        return Role.builder()
                .code(code.trim())
                .name(name)
                .description(description)
                .landlord(landlord)
                .rolePermissions(new HashSet<>())
                .build();
    }

    public static Role createNewWithGeneratedCode(String name, String description, Landlord landlord) {
        String generatedCode = name == null ? UUID.randomUUID().toString() :
                name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + UUID.randomUUID();
        return createNew(generatedCode, name, description, landlord);
    }

    public void setLandlord(Landlord landlord) {
         if (this.landlord != null && this.landlord != landlord) {
             this.landlord.getRoles().remove(this);
         }
         this.landlord = landlord;
         if (landlord != null && !landlord.getRoles().contains(this)) {
             landlord.getRoles().add(this);
         }
    }
    
    /**
     * Adiciona uma permission a este role mantendo a associação pivot
     */
    public void addPermission(Permission permission) {
        com.seccreto.service.auth.model.policies.Policy defaultPolicy =
                permission != null ? permission.getPolicy() : null;
        addPermission(permission, defaultPolicy);
    }

    public void addPermission(Permission permission, com.seccreto.service.auth.model.policies.Policy policy) {
        if (permission == null) {
            return;
        }
        if (this.rolePermissions == null) {
            this.rolePermissions = new HashSet<>();
        }
        if (permission.getLandlord() == null) {
            permission.setLandlord(this.landlord);
        } else if (!permission.getLandlord().equals(this.landlord)) {
            throw new IllegalArgumentException("Permissão pertence a outro landlord e não pode ser associada a este role");
        }
        com.seccreto.service.auth.model.policies.Policy effectivePolicy =
                policy != null ? policy : permission.getPolicy();
        RolesPermissions association = this.rolePermissions.stream()
                .filter(link -> link.getPermission().equals(permission))
                .findFirst()
                .orElse(null);

        if (association == null) {
            association = RolesPermissions.of(this, permission, effectivePolicy);
            this.rolePermissions.add(association);
        } else if (effectivePolicy != null) {
            association.attachPolicy(effectivePolicy);
        }
    }

    /**
     * Remove uma permission deste role mantendo a associação pivot
     */
    public void removePermission(Permission permission) {
        if (permission == null || this.rolePermissions == null) {
            return;
        }
        this.rolePermissions.removeIf(link -> link.getPermission().equals(permission));
    }

    /**
     * Verifica se este role tem uma permission específica
     */
    public boolean hasPermission(Permission permission) {
        return this.rolePermissions != null && this.rolePermissions.stream()
                .anyMatch(link -> link.getPermission().equals(permission));
    }

    /**
     * Retorna as permissions associadas a este role.
     */
    @Transient
    public Set<Permission> getPermissions() {
        if (this.rolePermissions == null) {
            return Set.of();
        }
        return this.rolePermissions.stream()
                .map(RolesPermissions::getPermission)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Expõe as associações role-permission para construção de DTOs/serviços.
     */
    public Set<RolesPermissions> getRolePermissions() {
        if (this.rolePermissions == null) {
            this.rolePermissions = new HashSet<>();
        }
        return this.rolePermissions;
    }
}
