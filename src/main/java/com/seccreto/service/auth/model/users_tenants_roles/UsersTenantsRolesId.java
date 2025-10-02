package com.seccreto.service.auth.model.users_tenants_roles;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Classe de chave primária composta para UsersTenantsRoles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsersTenantsRolesId implements Serializable {
    private UUID userId;
    private UUID tenantId;
    private UUID roleId;
}
