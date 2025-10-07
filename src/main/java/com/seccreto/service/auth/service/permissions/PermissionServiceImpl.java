package com.seccreto.service.auth.service.permissions;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.permissions.PermissionPolicyPresetResponse;
import com.seccreto.service.auth.api.dto.permissions.PermissionResponse;
import com.seccreto.service.auth.api.dto.roles_permissions.RolesPermissionsResponse;
import com.seccreto.service.auth.api.mapper.roles_permissions.RolesPermissionsMapper;
import com.seccreto.service.auth.api.mapper.permissions.PermissionMapper;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.repository.landlords.LandlordRepository;
import com.seccreto.service.auth.repository.permissions.PermissionRepository;
import com.seccreto.service.auth.repository.roles_permissions.RolesPermissionsRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final LandlordRepository landlordRepository;
    private final RolesPermissionsRepository rolesPermissionsRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository,
                                 LandlordRepository landlordRepository,
                                 RolesPermissionsRepository rolesPermissionsRepository) {
        this.permissionRepository = permissionRepository;
        this.landlordRepository = landlordRepository;
        this.rolesPermissionsRepository = rolesPermissionsRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "permissions.create", description = "Time taken to create a permission")
    public Permission createPermission(UUID landlordId, String action, String resource) {
        validateLandlordId(landlordId);
        validateAction(action);
        validateResource(resource);

        Landlord landlord = findLandlord(landlordId);

        // Verificar se já existe uma permissão com esta ação e recurso (idempotência)
        Optional<Permission> existingPermission = permissionRepository.findByLandlordIdAndActionAndResource(
                landlordId, action.trim(), resource.trim());
        if (existingPermission.isPresent()) {
            return existingPermission.get(); // Retorna a permissão existente (idempotência)
        }

        Permission permission = Permission.createNew(action.trim(), resource.trim(), landlord);
        return permissionRepository.save(permission);
    }

    @Override
    @Timed(value = "permissions.list", description = "Time taken to list permissions")
    public List<Permission> listPermissions(UUID landlordId) {
        validateLandlordId(landlordId);
        return permissionRepository.findByLandlordId(landlordId);
    }

    @Override
    @Timed(value = "permissions.find", description = "Time taken to find permission by id")
    public Optional<Permission> findPermissionById(UUID landlordId, UUID id) {
        validateLandlordId(landlordId);
        validateId(id);
        return permissionRepository.findById(id)
                .filter(permission -> permission.getLandlord() != null && landlordId.equals(permission.getLandlord().getId()));
    }

    @Override
    @Timed(value = "permissions.find", description = "Time taken to find permissions by action")
    public List<Permission> findPermissionsByAction(UUID landlordId, String action) {
        validateLandlordId(landlordId);
        validateAction(action);
        return permissionRepository.findByLandlordIdAndAction(landlordId, action);
    }

    @Override
    @Timed(value = "permissions.find", description = "Time taken to find permissions by resource")
    public List<Permission> findPermissionsByResource(UUID landlordId, String resource) {
        validateLandlordId(landlordId);
        validateResource(resource);
        return permissionRepository.findByLandlordIdAndResource(landlordId, resource);
    }

    @Override
    @Timed(value = "permissions.find", description = "Time taken to find permission by action and resource")
    public Optional<Permission> findPermissionByActionAndResource(UUID landlordId, String action, String resource) {
        validateLandlordId(landlordId);
        validateAction(action);
        validateResource(resource);
        return permissionRepository.findByLandlordIdAndActionAndResource(landlordId, action, resource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "permissions.update", description = "Time taken to update permission")
    public Permission updatePermission(UUID landlordId, UUID id, String action, String resource) {
        validateLandlordId(landlordId);
        validateId(id);
        validateAction(action);
        validateResource(resource);

        Permission permission = permissionRepository.findById(id)
                .filter(existing -> existing.getLandlord() != null && landlordId.equals(existing.getLandlord().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada para o landlord informado"));

        // Verificar se ação e recurso já existem em outra permissão
        Optional<Permission> existingPermission = permissionRepository.findByLandlordIdAndActionAndResource(landlordId, action.trim(), resource.trim());
        if (existingPermission.isPresent() && !existingPermission.get().getId().equals(id)) {
            throw new ConflictException("Ação e recurso já estão em uso por outra permissão deste landlord");
        }

        permission.setAction(action.trim());
        permission.setResource(resource.trim());

        return permissionRepository.save(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "permissions.delete", description = "Time taken to delete permission")
    public boolean deletePermission(UUID landlordId, UUID id) {
        validateLandlordId(landlordId);
        validateId(id);

        Permission permission = permissionRepository.findById(id)
                .filter(existing -> existing.getLandlord() != null && landlordId.equals(existing.getLandlord().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada para o landlord informado"));

        permissionRepository.delete(permission);
        return true;
    }

    @Override
    public boolean existsPermissionById(UUID landlordId, UUID id) {
        validateLandlordId(landlordId);
        validateId(id);
        return permissionRepository.findById(id)
                .map(permission -> permission.getLandlord() != null && landlordId.equals(permission.getLandlord().getId()))
                .orElse(false);
    }

    @Override
    public boolean existsPermissionByActionAndResource(UUID landlordId, String action, String resource) {
        validateLandlordId(landlordId);
        validateAction(action);
        validateResource(resource);
        return permissionRepository.existsByLandlordIdAndActionAndResource(landlordId, action, resource);
    }

    @Override
    @Timed(value = "permissions.count", description = "Time taken to count permissions")
    public long countPermissions(UUID landlordId) {
        validateLandlordId(landlordId);
    return permissionRepository.countByLandlordId(landlordId);
    }

    @Override
    public List<Permission> searchPermissions(UUID landlordId, String query) {
        validateLandlordId(landlordId);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return permissionRepository.search(landlordId, query.trim());
    }

    @Override
    public Pagination<PermissionResponse> searchPermissions(UUID landlordId, SearchQuery searchQuery) {
        validateLandlordId(landlordId);

        try {
            Pageable pageable = PageRequest.of(
                    Math.max(searchQuery.page() - 1, 0),
                    searchQuery.perPage(),
                    Sort.by(Sort.Direction.fromString(searchQuery.direction()), searchQuery.sort())
            );

            String terms = searchQuery.terms();
            if (terms == null) {
                terms = "";
            }

            Page<Permission> permissionPage = permissionRepository.search(landlordId, terms.trim(), pageable);

            List<PermissionResponse> permissionResponses = permissionPage.getContent().stream()
                    .map(PermissionMapper::toResponse)
                    .collect(Collectors.toList());

            return new Pagination<>(
                    searchQuery.page(),
                    searchQuery.perPage(),
                    permissionPage.getTotalElements(),
                    permissionResponses
            );
        } catch (Exception e) {
            return new Pagination<>(searchQuery.page(), searchQuery.perPage(), 0, List.of());
        }
    }

    @Override
    public List<RolesPermissionsResponse> getPermissionRoles(UUID landlordId, UUID permissionId) {
        validateLandlordId(landlordId);
        try {
        Permission permission = permissionRepository.findById(permissionId)
            .filter(p -> p.getLandlord() != null && landlordId.equals(p.getLandlord().getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada para o landlord informado"));

        return RolesPermissionsMapper.toResponseList(
            rolesPermissionsRepository.findByPermissionId(permission.getId())
        );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter roles da permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public long countPermissionRoles(UUID landlordId, UUID permissionId) {
        validateLandlordId(landlordId);
        try {
            Permission permission = permissionRepository.findById(permissionId)
                    .filter(p -> p.getLandlord() != null && landlordId.equals(p.getLandlord().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada para o landlord informado"));
            return permissionRepository.countRolesByPermission(permission.getId());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar roles da permissão: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PermissionPolicyPresetResponse> listPolicyPresets() {
        return SecurityPolicyPreset.toResponseList();
    }

    // Métodos de validação privados
    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("ID da permissão não pode ser nulo");
        }
    }

    private void validateLandlordId(UUID landlordId) {
        if (landlordId == null) {
            throw new ValidationException("ID do landlord é obrigatório");
        }
    }

    private Landlord findLandlord(UUID landlordId) {
        return landlordRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord não encontrado com ID: " + landlordId));
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