package com.seccreto.service.auth.service.permissions;

import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.repository.permissions.PermissionRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
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
 * Implementação da camada de serviço contendo regras de negócio para permissions.
 * Aplica SRP e DIP com transações declarativas.
 * Baseado na migração V5.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "permissions.create", description = "Time taken to create a permission")
    public Permission createPermission(String action, String resource) {
        validateAction(action);
        validateResource(resource);

        // Verificar se já existe uma permissão com esta ação e recurso (idempotência)
        Optional<Permission> existingPermission = permissionRepository.findByActionAndResource(action.trim(), resource.trim());
        if (existingPermission.isPresent()) {
            return existingPermission.get(); // Retorna a permissão existente (idempotência)
        }

        Permission permission = Permission.createNew(action.trim(), resource.trim());
        return permissionRepository.save(permission);
    }

    @Override
    @Timed(value = "permissions.list", description = "Time taken to list permissions")
    public List<Permission> listAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    @Timed(value = "permissions.find", description = "Time taken to find permission by id")
    public Optional<Permission> findPermissionById(UUID id) {
        validateId(id);
        return permissionRepository.findById(id);
    }

    @Override
    @Timed(value = "permissions.find", description = "Time taken to find permissions by action")
    public List<Permission> findPermissionsByAction(String action) {
        validateAction(action);
        return permissionRepository.findByAction(action);
    }

    @Override
    @Timed(value = "permissions.find", description = "Time taken to find permissions by resource")
    public List<Permission> findPermissionsByResource(String resource) {
        validateResource(resource);
        return permissionRepository.findByResource(resource);
    }

    @Override
    @Timed(value = "permissions.find", description = "Time taken to find permission by action and resource")
    public Optional<Permission> findPermissionByActionAndResource(String action, String resource) {
        validateAction(action);
        validateResource(resource);
        return permissionRepository.findByActionAndResource(action, resource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "permissions.update", description = "Time taken to update permission")
    public Permission updatePermission(UUID id, String action, String resource) {
        validateId(id);
        validateAction(action);
        validateResource(resource);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada com ID: " + id));

        // Verificar se ação e recurso já existem em outra permissão
        Optional<Permission> existingPermission = permissionRepository.findByActionAndResource(action.trim(), resource.trim());
        if (existingPermission.isPresent() && !existingPermission.get().getId().equals(id)) {
            throw new ConflictException("Ação e recurso já estão em uso por outra permissão");
        }

        permission.setAction(action.trim());
        permission.setResource(resource.trim());

        return permissionRepository.save(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "permissions.delete", description = "Time taken to delete permission")
    public boolean deletePermission(UUID id) {
        validateId(id);
        
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permissão não encontrada com ID: " + id);
        }

        permissionRepository.deleteById(id);
        return true;
    }

    @Override
    public boolean existsPermissionById(UUID id) {
        validateId(id);
        return permissionRepository.existsById(id);
    }

    @Override
    public boolean existsPermissionByActionAndResource(String action, String resource) {
        validateAction(action);
        validateResource(resource);
        return permissionRepository.existsByActionAndResource(action, resource);
    }

    @Override
    @Timed(value = "permissions.count", description = "Time taken to count permissions")
    public long countPermissions() {
        return permissionRepository.count();
    }

    @Override
    public List<Permission> searchPermissions(String query) {
        return permissionRepository.search(query);
    }

    @Override
    public List<Object> getPermissionRoles(UUID permissionId) {
        try {
            List<Object[]> results = permissionRepository.getPermissionRolesDetails(permissionId);
            return results.stream()
                    .map(row -> java.util.Map.of(
                        "id", row[0],
                        "name", row[1],
                        "description", row[2],
                        "policyId", row[3]
                    ))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter roles da permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public long countPermissionRoles(UUID permissionId) {
        try {
            return permissionRepository.countRolesByPermission(permissionId);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar roles da permissão: " + e.getMessage(), e);
        }
    }

    // Métodos de validação privados
    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("ID da permissão não pode ser nulo");
        }
    }

    private void validateAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new ValidationException("Ação da permissão é obrigatória");
        }
        if (action.trim().length() < 2) {
            throw new ValidationException("Ação da permissão deve ter pelo menos 2 caracteres");
        }
    }

    private void validateResource(String resource) {
        if (resource == null || resource.trim().isEmpty()) {
            throw new ValidationException("Recurso da permissão é obrigatório");
        }
        if (resource.trim().length() < 2) {
            throw new ValidationException("Recurso da permissão deve ter pelo menos 2 caracteres");
        }
    }
}