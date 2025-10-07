package com.seccreto.service.auth.model.users_tenants_permissions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.model.users.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

/**
 * UsersTenantsPermissions - Tabela pivot para permissões diretas por tenant.
 */
@Entity
@Table(name = "users_tenants_permissions",
       indexes = {
           @Index(name = "idx_utp_user_id", columnList = "user_id"),
           @Index(name = "idx_utp_tenant_id", columnList = "tenant_id"),
           @Index(name = "idx_utp_permission_id", columnList = "permission_id"),
           @Index(name = "idx_utp_user_tenant", columnList = "user_id, tenant_id"),
           @Index(name = "idx_utp_tenant_permission", columnList = "tenant_id, permission_id")
       })
@IdClass(UsersTenantsPermissionsId.class)
@Schema(description = "Tabela pivot: User <-> Tenant <-> Permission")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"user", "tenant", "permission"})
public class UsersTenantsPermissions implements Serializable {

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
    @Column(name = "permission_id", nullable = false)
    @Schema(description = "FK para Permission")
    @EqualsAndHashCode.Include
    private UUID permissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private Permission permission;

    public static UsersTenantsPermissions createNew(UUID userId, UUID tenantId, UUID permissionId) {
        return UsersTenantsPermissions.builder()
                .userId(userId)
                .tenantId(tenantId)
                .permissionId(permissionId)
                .build();
    }

    public static UsersTenantsPermissions of(UUID userId, UUID tenantId, UUID permissionId) {
        return createNew(userId, tenantId, permissionId);
    }

    public static UsersTenantsPermissions of(User user, Tenant tenant, Permission permission) {
        UsersTenantsPermissions association = createNew(user.getId(), tenant.getId(), permission.getId());
        association.setUser(user);
        association.setTenant(tenant);
        association.setPermission(permission);
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
            if (this.permission != null && this.permission.getLandlord() != null && tenant.getLandlord() != null
                    && !this.permission.getLandlord().getId().equals(tenant.getLandlord().getId())) {
                throw new IllegalArgumentException("Permissão e Tenant pertencem a landlords diferentes");
            }
        }
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
        if (permission != null) {
            this.permissionId = permission.getId();
            if (this.tenant != null && permission.getLandlord() != null && this.tenant.getLandlord() != null
                    && !permission.getLandlord().getId().equals(this.tenant.getLandlord().getId())) {
                throw new IllegalArgumentException("Permissão e Tenant pertencem a landlords diferentes");
            }
        }
    }
}
