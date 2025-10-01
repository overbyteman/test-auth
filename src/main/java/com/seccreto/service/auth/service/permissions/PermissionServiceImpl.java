package com.seccreto.service.auth.service.permissions;

import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.repository.permissions.PermissionRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementação da camada de serviço contendo regras de negócio para permissões.
 * Aplica SRP e DIP com transações declarativas.
 * 
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a RBAC (Role-Based Access Control)
 * - Validação de unicidade action+resource
 * - Versioning para optimistic locking
 */
@Service
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
        
        // Verificar se já existe uma permissão com esta combinação (idempotência)
        Optional<Permission> existingPermission = permissionRepository.findByActionAndResourceExact(action.trim(), resource.trim());
        if (existingPermission.isPresent()) {
            return existingPermission.get(); // Retorna a permissão existente (idempotência)
        }
        
        Permission permission = Permission.createNew(action.trim(), resource.trim());
        Permission savedPermission = permissionRepository.save(permission);
        return savedPermission;
    }

    @Override
    public List<Permission> listAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    public Optional<Permission> findPermissionById(Long id) {
        validateId(id);
        return permissionRepository.findById(id);
    }

    @Override
    public List<Permission> findPermissionsByAction(String action) {
        validateAction(action);
        return permissionRepository.findByAction(action.trim());
    }

    @Override
    public List<Permission> findPermissionsByResource(String resource) {
        validateResource(resource);
        return permissionRepository.findByResource(resource.trim());
    }

    @Override
    public List<Permission> findPermissionsByActionAndResource(String action, String resource) {
        validateAction(action);
        validateResource(resource);
        return permissionRepository.findByActionAndResource(action.trim(), resource.trim());
    }

    @Override
    public Optional<Permission> findPermissionByActionAndResourceExact(String action, String resource) {
        validateAction(action);
        validateResource(resource);
        return permissionRepository.findByActionAndResourceExact(action.trim(), resource.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "permissions.update", description = "Time taken to update a permission")
    public Permission updatePermission(Long id, String action, String resource) {
        validateId(id);
        validateAction(action);
        validateResource(resource);
        
        Permission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada com ID: " + id));
        
        // Verificar se os dados são diferentes (idempotência)
        if (existing.getAction().equals(action.trim()) && existing.getResource().equals(resource.trim())) {
            return existing; // Retorna a permissão sem alterações (idempotência)
        }
        
        // Verificar se a combinação action+resource já está em uso por outra permissão
        permissionRepository.findByActionAndResourceExact(action.trim(), resource.trim()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new ConflictException("Já existe uma permissão com esta combinação de ação e recurso");
            }
        });
        
        existing.setAction(action.trim());
        existing.setResource(resource.trim());
        Permission updatedPermission = permissionRepository.update(existing);
        return updatedPermission;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "permissions.delete", description = "Time taken to delete a permission")
    public boolean deletePermission(Long id) {
        validateId(id);
        
        // Verificar se a permissão existe antes de tentar deletar (idempotência)
        if (!permissionRepository.existsById(id)) {
            return false; // Permissão já não existe (idempotência)
        }
        
        boolean deleted = permissionRepository.deleteById(id);
        return deleted;
    }

    @Override
    public boolean existsPermissionById(Long id) {
        validateId(id);
        return permissionRepository.existsById(id);
    }

    @Override
    public boolean existsPermissionByActionAndResource(String action, String resource) {
        validateAction(action);
        validateResource(resource);
        return permissionRepository.existsByActionAndResource(action.trim(), resource.trim());
    }

    @Override
    public long countPermissions() {
        return permissionRepository.count();
    }

    @Override
    public List<Permission> searchPermissions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of(); // Retorna lista vazia para query inválida
        }
        return permissionRepository.search(query.trim());
    }

    private void validateAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new ValidationException("Ação não pode ser vazia");
        }
        if (action.trim().length() < 2) {
            throw new ValidationException("Ação deve ter pelo menos 2 caracteres");
        }
    }

    private void validateResource(String resource) {
        if (resource == null || resource.trim().isEmpty()) {
            throw new ValidationException("Recurso não pode ser vazio");
        }
        if (resource.trim().length() < 2) {
            throw new ValidationException("Recurso deve ter pelo menos 2 caracteres");
        }
    }

    private void validateId(Long id) {
        if (id == null) {
            throw new ValidationException("ID não pode ser nulo");
        }
        if (id <= 0) {
            throw new ValidationException("ID deve ser maior que zero");
        }
    }
}
