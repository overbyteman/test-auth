package com.seccreto.service.auth.service.roles_permissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.repository.roles_permissions.RolesPermissionsRepository;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação da camada de serviço contendo regras de negócio para roles_permissions.
 * Aplica SRP e DIP com transações declarativas.
 * Baseado na migração V6.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class RolesPermissionsServiceImpl implements RolesPermissionsService {

    private final RolesPermissionsRepository rolesPermissionsRepository;
    public RolesPermissionsServiceImpl(RolesPermissionsRepository rolesPermissionsRepository) {
        this.rolesPermissionsRepository = rolesPermissionsRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles_permissions.create", description = "Time taken to create a role permission")
    public RolesPermissions createRolePermission(UUID roleId, UUID permissionId) {
        validateRoleId(roleId);
        validatePermissionId(permissionId);

        // Verificar se já existe a relação (idempotência)
        Optional<RolesPermissions> existingRelation = rolesPermissionsRepository.findByRoleIdAndPermissionId(roleId, permissionId);
        if (existingRelation.isPresent()) {
            return existingRelation.get(); // Retorna a relação existente (idempotência)
        }

        RolesPermissions rolesPermissions = new RolesPermissions();
        rolesPermissions.setRoleId(roleId);
        rolesPermissions.setPermissionId(permissionId);

        return rolesPermissionsRepository.save(rolesPermissions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles_permissions.create", description = "Time taken to create a role permission association")
    public RolesPermissions createAssociation(UUID roleId, UUID permissionId) {
        return createRolePermission(roleId, permissionId);
    }

    @Override
    @Timed(value = "roles_permissions.list", description = "Time taken to list role permissions")
    public List<RolesPermissions> listAllRolePermissions() {
        return rolesPermissionsRepository.findAll();
    }

    @Override
    @Timed(value = "roles_permissions.find", description = "Time taken to find role permission")
    public Optional<RolesPermissions> findRolePermission(UUID roleId, UUID permissionId) {
        validateRoleId(roleId);
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.findByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    @Timed(value = "roles_permissions.find", description = "Time taken to find permissions by role")
    public List<RolesPermissions> findPermissionsByRole(UUID roleId) {
        validateRoleId(roleId);
        return rolesPermissionsRepository.findByRoleId(roleId);
    }

    @Override
    @Timed(value = "roles_permissions.find", description = "Time taken to find roles by permission")
    public List<RolesPermissions> findRolesByPermission(UUID permissionId) {
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.findByPermissionId(permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles_permissions.delete", description = "Time taken to delete role permission")
    public boolean deleteRolePermission(UUID roleId, UUID permissionId) {
        validateRoleId(roleId);
        validatePermissionId(permissionId);
        rolesPermissionsRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles_permissions.delete", description = "Time taken to remove role permission association")
    public boolean removeAssociation(UUID roleId, UUID permissionId) {
        return deleteRolePermission(roleId, permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAllPermissionsByRole(UUID roleId) {
        validateRoleId(roleId);
        rolesPermissionsRepository.deleteByRoleId(roleId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAllRolesByPermission(UUID permissionId) {
        validatePermissionId(permissionId);
        rolesPermissionsRepository.deleteByPermissionId(permissionId);
        return true;
    }

    @Override
    public boolean existsRolePermission(UUID roleId, UUID permissionId) {
        validateRoleId(roleId);
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.existsByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    public boolean existsPermissionsByRole(UUID roleId) {
        validateRoleId(roleId);
        return rolesPermissionsRepository.existsByRoleId(roleId);
    }

    @Override
    public boolean existsRolesByPermission(UUID permissionId) {
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.existsByPermissionId(permissionId);
    }

    @Override
    @Timed(value = "roles_permissions.count", description = "Time taken to count role permissions")
    public long countRolePermissions() {
        return rolesPermissionsRepository.count();
    }

    @Override
    @Timed(value = "roles_permissions.count", description = "Time taken to count associations")
    public long countAssociations() {
        return rolesPermissionsRepository.count();
    }

    @Override
    public long countPermissionsByRole(UUID roleId) {
        validateRoleId(roleId);
        return rolesPermissionsRepository.countByRoleId(roleId);
    }

    @Override
    public long countRolesByPermission(UUID permissionId) {
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.countByPermissionId(permissionId);
    }

    @Override
    public boolean roleHasPermission(UUID roleId, UUID permissionId) {
        validateRoleId(roleId);
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.existsByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    public boolean roleHasPermissionByActionAndResource(UUID roleId, String action, String resource) {
        try {
            return rolesPermissionsRepository.roleHasPermissionByActionAndResource(roleId, action, resource);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar permissão do role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getRolePermissionsDetails(UUID roleId) {
        try {
            List<Object[]> results = rolesPermissionsRepository.getRolePermissionsDetails(roleId);
            return results.stream()
                    .map(row -> java.util.Map.of(
                        "id", row[0],
                        "action", row[1],
                        "resource", row[2]
                    ))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter detalhes das permissões do role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getPermissionRolesDetails(UUID permissionId) {
        try {
            List<Object[]> results = rolesPermissionsRepository.getPermissionRolesDetails(permissionId);
            return results.stream()
                    .map(row -> java.util.Map.of(
                        "id", row[0],
                        "name", row[1],
                        "description", row[2]
                    ))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter detalhes dos roles da permissão: " + e.getMessage(), e);
        }
    }

    // Métodos de validação privados
    private void validateRoleId(UUID roleId) {
        if (roleId == null) {
            throw new ValidationException("ID do role não pode ser nulo");
        }
    }

    private void validatePermissionId(UUID permissionId) {
        if (permissionId == null) {
            throw new ValidationException("ID da permissão não pode ser nulo");
        }
    }
}