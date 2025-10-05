package com.seccreto.service.auth.model.users_tenants_roles;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Classe de chave primária composta para UsersTenantsRoles
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsersTenantsRolesId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;
}
