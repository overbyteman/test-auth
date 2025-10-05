package com.seccreto.service.auth.model.users_tenants_roles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.model.users.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * UsersTenantsRoles - Tabela Pivot Multi-Tenancy (Normalizada)
 *
 * ESTRUTURA NORMALIZADA (3NF):
 * - PK Composta: (user_id, tenant_id, role_id)
 * - FK: user_id -> users(id)
 * - FK: tenant_id -> tenants(id)
 * - FK: role_id -> roles(id)
 *
 * TABELA PIVOT PARA:
 * - Relacionamento N:N:N entre User, Tenant e Role
 * - Um usuário pode ter múltiplos roles em um tenant
 * - Um usuário pode pertencer a múltiplos tenants
 *
 * OTIMIZAÇÕES:
 * - Índices automáticos nas FKs
 * - Índices compostos para queries comuns
 * - PK composta para unicidade e performance
 */
@Entity
@Table(name = "users_tenants_roles",
       indexes = {
           @Index(name = "idx_utr_user_id", columnList = "user_id"),
           @Index(name = "idx_utr_tenant_id", columnList = "tenant_id"),
           @Index(name = "idx_utr_role_id", columnList = "role_id"),
           @Index(name = "idx_utr_user_tenant", columnList = "user_id, tenant_id"),
           @Index(name = "idx_utr_tenant_role", columnList = "tenant_id, role_id")
       })
@IdClass(UsersTenantsRolesId.class)
@Schema(description = "Tabela pivot: User <-> Tenant <-> Role (normalizada)")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"user", "tenant", "role"})
public class UsersTenantsRoles implements Serializable {

    @Id
    @Column(name = "user_id", nullable = false)
    @Schema(description = "FK para User")
    @EqualsAndHashCode.Include
    private UUID userId;
    
    @Id
    @Column(name = "tenant_id", nullable = false)
    @Schema(description = "FK para Tenant")
    @EqualsAndHashCode.Include
    private UUID tenantId;
    
    @Id
    @Column(name = "role_id", nullable = false)
    @Schema(description = "FK para Role")
    @EqualsAndHashCode.Include
    private UUID roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    @Schema(description = "Usuário associado")
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, insertable = false, updatable = false)
    @Schema(description = "Tenant associado")
    @JsonIgnore
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, insertable = false, updatable = false)
    @Schema(description = "Role associado")
    @JsonIgnore
    private Role role;

    // ========================================
    // FACTORY METHODS
    // ========================================

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
        return createNew(userId, tenantId, roleId);
    }

    /**
     * Factory method com entidades
     */
    public static UsersTenantsRoles of(User user, Tenant tenant, Role role) {
        UsersTenantsRoles association = createNew(user.getId(), tenant.getId(), role.getId());
        association.setUser(user);
        association.setTenant(tenant);
        association.setRole(role);
        return association;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
        if (tenant != null) {
            this.tenantId = tenant.getId();
        }
    }

    public void setRole(Role role) {
        this.role = role;
        if (role != null) {
            this.roleId = role.getId();
        }
    }
}