package com.seccreto.service.auth.model.users_tenants_roles;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Classe que representa a relação many-to-many entre users, tenants e roles (JPA Entity)
 *
 * Características de implementação sênior:
 * - JPA Entity com chave primária composta
 * - Tabela de junção para multi-tenancy
 * - Suporte a multi-tenancy com RBAC
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Entity
@Table(name = "users_tenants_roles")
@IdClass(UsersTenantsRolesId.class)
@Schema(description = "Entidade que representa a relação many-to-many entre users, tenants e roles para multi-tenancy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class UsersTenantsRoles {
    @Id
    @Column(name = "user_id")
    @Schema(description = "ID do usuário (UUID)")
    @EqualsAndHashCode.Include
    private UUID userId;
    
    @Id
    @Column(name = "tenant_id")
    @Schema(description = "ID do tenant (UUID)")
    @EqualsAndHashCode.Include
    private UUID tenantId;
    
    @Id
    @Column(name = "role_id")
    @Schema(description = "ID do role (UUID)")
    @EqualsAndHashCode.Include
    private UUID roleId;

    /**
     * Construtor para criação de novas relações user-tenant-role
     */
    public static UsersTenantsRoles createNew(UUID userId, UUID tenantId, UUID roleId) {
        return UsersTenantsRoles.builder()
                .userId(userId)
                .tenantId(tenantId)
                .roleId(roleId)
                .build();
    }

    /**
     * Factory method for creating new relationships
     */
    public static UsersTenantsRoles of(UUID userId, UUID tenantId, UUID roleId) {
        return UsersTenantsRoles.builder()
                .userId(userId)
                .tenantId(tenantId)
                .roleId(roleId)
                .build();
    }
}