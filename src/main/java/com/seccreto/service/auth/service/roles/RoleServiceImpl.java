package com.seccreto.service.auth.service.roles;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.roles.RoleResponse;
import com.seccreto.service.auth.api.mapper.roles.RoleMapper;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.repository.roles.RoleRepository;
import com.seccreto.service.auth.repository.landlords.LandlordRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementação da camada de serviço contendo regras de negócio para roles.
 * Aplica SRP e DIP com transações declarativas.
 * Baseado na migração V4.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final LandlordRepository landlordRepository;

    public RoleServiceImpl(RoleRepository roleRepository, LandlordRepository landlordRepository) {
        this.roleRepository = roleRepository;
        this.landlordRepository = landlordRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.create", description = "Time taken to create a role")
    public Role createRole(UUID landlordId, String code, String name, String description) {
        validateLandlordId(landlordId);
        validateCode(code);
        validateName(name);

        Landlord landlord = findLandlord(landlordId);

        roleRepository.findByCodeAndLandlordId(code.trim(), landlordId)
                .ifPresent(existing -> { throw new ConflictException("Code já está em uso para este landlord"); });

        roleRepository.findByNameAndLandlordId(name.trim(), landlordId)
                .ifPresent(existing -> { throw new ConflictException("Nome já está em uso para este landlord"); });

        Role role = Role.createNew(code.trim(), name.trim(), description, landlord);
        return roleRepository.save(role);
    }

    @Override
    @Timed(value = "roles.list", description = "Time taken to list roles")
    public List<Role> listRoles(UUID landlordId) {
        validateLandlordId(landlordId);
        return roleRepository.findByLandlordId(landlordId);
    }

    @Override
    @Timed(value = "roles.find", description = "Time taken to find role by id")
    public Optional<Role> findRoleById(UUID landlordId, UUID id) {
        validateLandlordId(landlordId);
        validateId(id);
        return roleRepository.findByIdWithTenant(id)
                .filter(role -> role.getLandlord() != null && landlordId.equals(role.getLandlord().getId()));
    }

    @Override
    public Optional<Role> findRoleByCode(UUID landlordId, String code) {
        validateLandlordId(landlordId);
        validateCode(code);
        return roleRepository.findByCodeAndLandlordId(code.trim(), landlordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.update", description = "Time taken to update role")
    public Role updateRole(UUID landlordId, UUID id, String name, String description) {
        validateLandlordId(landlordId);
        validateId(id);
        validateName(name);

        Role role = requireRole(landlordId, id);

        roleRepository.findByNameAndLandlordId(name.trim(), landlordId)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new ConflictException("Nome já está em uso para este landlord"); });

        if (description != null && description.trim().isEmpty()) {
            description = null;
        }

        role.setName(name.trim());
        role.setDescription(description);

        return roleRepository.save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.delete", description = "Time taken to delete role")
    public boolean deleteRole(UUID landlordId, UUID id) {
        validateLandlordId(landlordId);
        validateId(id);

        Role role = requireRole(landlordId, id);
        roleRepository.delete(role);
        return true;
    }

    @Override
    public boolean existsRoleById(UUID landlordId, UUID id) {
        validateLandlordId(landlordId);
        validateId(id);
        return roleRepository.findById(id)
                .map(role -> role.getLandlord() != null && landlordId.equals(role.getLandlord().getId()))
                .orElse(false);
    }

    @Override
    public boolean existsRoleByCode(UUID landlordId, String code) {
        validateLandlordId(landlordId);
        validateCode(code);
        return roleRepository.existsByCodeAndLandlordId(code.trim(), landlordId);
    }

    @Override
    @Timed(value = "roles.count", description = "Time taken to count roles")
    public long countRoles(UUID landlordId) {
        validateLandlordId(landlordId);
        return roleRepository.countByLandlordId(landlordId);
    }

    @Override
    public List<Role> searchRoles(UUID landlordId, String query) {
        validateLandlordId(landlordId);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return roleRepository.search(landlordId, query.trim());
    }

    @Override
    public Pagination<RoleResponse> searchRoles(UUID landlordId, SearchQuery searchQuery) {
        validateLandlordId(landlordId);

        try {
            Pageable pageable = PageRequest.of(
                searchQuery.page() - 1,
                searchQuery.perPage(),
                Sort.by(Sort.Direction.fromString(searchQuery.direction()), searchQuery.sort())
            );

            Page<Role> rolePage = roleRepository.search(landlordId, searchQuery.terms(), pageable);

            List<RoleResponse> roleResponses = rolePage.getContent().stream()
                .map(RoleMapper::toResponse)
                .collect(Collectors.toList());

            return new Pagination<>(
                searchQuery.page(),
                searchQuery.perPage(),
                rolePage.getTotalElements(),
                roleResponses
            );
        } catch (Exception e) {
            return new Pagination<>(searchQuery.page(), searchQuery.perPage(), 0, List.of());
        }
    }

    @Override
    public List<Object> getRolePermissions(UUID landlordId, UUID roleId) {
        Role role = requireRole(landlordId, roleId);
        try {
            List<Object[]> results = roleRepository.getRolePermissionsDetails(role.getId());
            return results.stream()
                    .map(row -> Map.of(
                            "id", row[0],
                            "action", row[1],
                            "resource", row[2],
                            "policyId", row[3]
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter permissões do role: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean roleHasPermission(UUID landlordId, UUID roleId, String action, String resource) {
        Role role = requireRole(landlordId, roleId);
        try {
            return roleRepository.roleHasPermission(role.getId(), action, resource);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar permissão do role: " + e.getMessage(), e);
        }
    }

    @Override
    public long countRolePermissions(UUID landlordId, UUID roleId) {
        Role role = requireRole(landlordId, roleId);
        try {
            return roleRepository.countPermissionsByRole(role.getId());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar permissões do role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getRoleUsers(UUID landlordId, UUID roleId) {
        Role role = requireRole(landlordId, roleId);
        try {
            List<Object[]> results = roleRepository.getRoleUsersDetails(role.getId());
            return results.stream()
                    .map(row -> Map.of(
                            "id", row[0],
                            "name", row[1],
                            "email", row[2],
                            "isActive", row[3]
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter usuários do role: " + e.getMessage(), e);
        }
    }

    @Override
    public long countRoleUsers(UUID landlordId, UUID roleId) {
        Role role = requireRole(landlordId, roleId);
        try {
            return roleRepository.countUsersByRole(role.getId());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar usuários do role: " + e.getMessage(), e);
        }
    }

    // Métodos de validação privados
    private void validateLandlordId(UUID landlordId) {
        if (landlordId == null) {
            throw new IllegalArgumentException("LandlordId não pode ser nulo");
        }
    }

    private void validateId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID do role não pode ser nulo");
        }
    }

    private void validateCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code do role é obrigatório");
        }
        if (code.trim().length() < 2) {
            throw new IllegalArgumentException("Code do role deve ter pelo menos 2 caracteres");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do role é obrigatório");
        }
        if (name.trim().length() < 2) {
            throw new IllegalArgumentException("Nome do role deve ter pelo menos 2 caracteres");
        }
    }

    private Landlord findLandlord(UUID landlordId) {
        return landlordRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord não encontrado com ID: " + landlordId));
    }

    private Role requireRole(UUID landlordId, UUID roleId) {
        return roleRepository.findById(roleId)
                .filter(role -> role.getLandlord() != null && landlordId.equals(role.getLandlord().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role não encontrado para o landlord informado (landlordId=" + landlordId + ", roleId=" + roleId + ")"));
    }
}
