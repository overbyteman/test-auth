package com.seccreto.service.auth.service.auth;

import java.util.List;
import java.util.UUID;

/**
 * Dados agregados de acesso a tenant para um usuário.
 */
public record TenantAccess(
        UUID tenantId,
        String tenantName,
        List<TenantRoleAccess> roles
) {

    public record TenantRoleAccess(
            String roleName,
            List<String> permissions
    ) {
    }
}
