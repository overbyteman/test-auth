package com.seccreto.service.auth.service.roles;

import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.repository.roles.RoleRepository;
import com.seccreto.service.auth.repository.tenants.TenantRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
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
    private final TenantRepository tenantRepository;

    public RoleServiceImpl(RoleRepository roleRepository, TenantRepository tenantRepository) {
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.create", description = "Time taken to create a role")
    public Role createRole(UUID tenantId, String code, String name, String description) {
        validateTenantId(tenantId);
        validateCode(code);
        validateName(name);

        Tenant tenant = findTenant(tenantId);

        roleRepository.findByCodeAndTenantId(code.trim(), tenantId)
                .ifPresent(existing -> { throw new ConflictException("Code já está em uso para este tenant"); });

        roleRepository.findByNameAndTenantId(name.trim(), tenantId)
                .ifPresent(existing -> { throw new ConflictException("Nome já está em uso para este tenant"); });

        Role role = Role.createNew(code.trim(), name.trim(), description, tenant);
        return roleRepository.save(role);
    }

    @Override
    @Timed(value = "roles.list", description = "Time taken to list roles")
    public List<Role> listRoles(UUID tenantId) {
        validateTenantId(tenantId);
        return roleRepository.findByTenantId(tenantId);
    }

    @Override
    @Timed(value = "roles.find", description = "Time taken to find role by id")
    public Optional<Role> findRoleById(UUID tenantId, UUID id) {
        validateTenantId(tenantId);
        validateId(id);
        return roleRepository.findById(id)
                .filter(role -> role.getTenant() != null && tenantId.equals(role.getTenant().getId()));
    }

    @Override
    public Optional<Role> findRoleByCode(UUID tenantId, String code) {
        validateTenantId(tenantId);
        validateCode(code);
        return roleRepository.findByCodeAndTenantId(code.trim(), tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.update", description = "Time taken to update role")
    public Role updateRole(UUID tenantId, UUID id, String name, String description) {
        validateTenantId(tenantId);
        validateId(id);
        validateName(name);

        Role role = requireRole(tenantId, id);

        roleRepository.findByNameAndTenantId(name.trim(), tenantId)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new ConflictException("Nome já está em uso para este tenant"); });

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
    public boolean deleteRole(UUID tenantId, UUID id) {
        validateTenantId(tenantId);
        validateId(id);

        Role role = requireRole(tenantId, id);
        roleRepository.delete(role);
        return true;
    }

    @Override
    public boolean existsRoleById(UUID tenantId, UUID id) {
        validateTenantId(tenantId);
        validateId(id);
        return roleRepository.findById(id)
                .map(role -> role.getTenant() != null && tenantId.equals(role.getTenant().getId()))
                .orElse(false);
    }

    @Override
    public boolean existsRoleByCode(UUID tenantId, String code) {
        validateTenantId(tenantId);
        validateCode(code);
        return roleRepository.existsByCodeAndTenantId(code.trim(), tenantId);
    }

    @Override
    @Timed(value = "roles.count", description = "Time taken to count roles")
    public long countRoles(UUID tenantId) {
        validateTenantId(tenantId);
        return roleRepository.countByTenantId(tenantId);
    }

    @Override
    public List<Role> searchRoles(UUID tenantId, String query) {
        validateTenantId(tenantId);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return roleRepository.search(tenantId, query.trim());
    }

    @Override
    public List<Object> getRolePermissions(UUID tenantId, UUID roleId) {
        Role role = requireRole(tenantId, roleId);
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
    public boolean roleHasPermission(UUID tenantId, UUID roleId, String action, String resource) {
        Role role = requireRole(tenantId, roleId);
        try {
            return roleRepository.roleHasPermission(role.getId(), action, resource);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar permissão do role: " + e.getMessage(), e);
        }
    }

    @Override
    public long countRolePermissions(UUID tenantId, UUID roleId) {
        Role role = requireRole(tenantId, roleId);
        try {
            return roleRepository.countPermissionsByRole(role.getId());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar permissões do role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getRoleUsers(UUID tenantId, UUID roleId) {
        Role role = requireRole(tenantId, roleId);
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
    public long countRoleUsers(UUID tenantId, UUID roleId) {
        Role role = requireRole(tenantId, roleId);
        try {
            return roleRepository.countUsersByRole(role.getId());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar usuários do role: " + e.getMessage(), e);
        }
    }

    // Métodos de validação privados
    private void validateTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId não pode ser nulo");
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

    private Tenant findTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + tenantId));
    }

    private Role requireRole(UUID tenantId, UUID roleId) {
        return roleRepository.findById(roleId)
                .filter(role -> role.getTenant() != null && tenantId.equals(role.getTenant().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role não encontrado para o tenant informado (tenantId=" + tenantId + ", roleId=" + roleId + ")"));
    }
}
