package com.seccreto.service.auth.model.roles_permissions;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Classe de chave primária composta para RolesPermissions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RolesPermissionsId implements Serializable {
    private UUID roleId;
    private UUID permissionId;
}
