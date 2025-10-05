package com.seccreto.service.auth.model.roles_permissions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.roles.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidade que representa o relacionamento pivô entre Role e Permission.
 * Permite anexar uma policy específica para a permissão dentro de um role.
 */
@Entity
@Table(name = "roles_permissions",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"role_id", "permission_id"})
       })
@Schema(description = "Pivot Role <-> Permission com suporte a policy por associação")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"role", "permission", "policy"})
public class RolesPermissions {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private RolesPermissionsId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    @Schema(description = "Role associado")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id", nullable = false)
    @Schema(description = "Permission associado")
    private Permission permission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    @Schema(description = "Policy específica desta associação role-permission")
    private Policy policy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Data de criação da associação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data da última atualização da associação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static RolesPermissions of(Role role, Permission permission) {
        return of(role, permission, null);
    }

    public static RolesPermissions of(Role role, Permission permission, Policy policy) {
        RolesPermissionsId id = new RolesPermissionsId(role.getId(), permission.getId());
        return RolesPermissions.builder()
                .id(id)
                .role(role)
                .permission(permission)
                .policy(policy)
                .build();
    }

    public void attachPolicy(Policy policy) {
        this.policy = policy;
    }

    public void clearPolicy() {
        this.policy = null;
    }
}
