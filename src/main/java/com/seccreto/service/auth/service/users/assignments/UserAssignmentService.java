package com.seccreto.service.auth.service.users.assignments;

import java.util.List;
import java.util.UUID;

/**
 * Serviço orquestrador responsável por atribuir roles e permissões a usuários em um tenant específico.
 * Consolida as operações das tabelas pivô garantindo consistência transacional e propagação de permissões.
 */
public interface UserAssignmentService {

    AssignmentResult assignRoles(UUID userId, UUID tenantId, List<UUID> roleIds);

    /**
     * Resultado consolidado de uma operação de atribuição.
     */
    record AssignmentResult(
        UUID userId,
        UUID tenantId,
        List<UUID> requestedRoleIds,
        List<UUID> newlyAssignedRoleIds,
        List<UUID> alreadyAssignedRoleIds,
        List<UUID> requestedPermissionIds,
        List<UUID> newlyAssignedPermissionIds,
        List<UUID> alreadyAssignedPermissionIds,
        List<UUID> propagatedPermissionIds
    ) { }
}
