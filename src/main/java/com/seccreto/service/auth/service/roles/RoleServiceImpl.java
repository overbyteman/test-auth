package com.seccreto.service.auth.service.roles;

import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.repository.roles.RoleRepository;
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
 * Implementação da camada de serviço contendo regras de negócio para roles.
 * Aplica SRP e DIP com transações declarativas.
 * Baseado na migração V4.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.create", description = "Time taken to create a role")
    public Role createRole(String name, String description) {
        validateName(name);

        // Verificar se já existe um role com este nome (idempotência)
        Optional<Role> existingRole = roleRepository.findByNameExact(name.trim());
        if (existingRole.isPresent()) {
            return existingRole.get(); // Retorna o role existente (idempotência)
        }

        Role role = Role.createNew(name.trim(), description);
        return roleRepository.save(role);
    }

    @Override
    @Timed(value = "roles.list", description = "Time taken to list roles")
    public List<Role> listAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Timed(value = "roles.find", description = "Time taken to find role by id")
    public Optional<Role> findRoleById(UUID id) {
        validateId(id);
        return roleRepository.findById(id);
    }

    @Override
    @Timed(value = "roles.find", description = "Time taken to find roles by name")
    public List<Role> findRolesByName(String name) {
        validateName(name);
        Optional<Role> role = roleRepository.findByName(name);
        return role.map(List::of).orElse(List.of());
    }

    @Override
    @Timed(value = "roles.find", description = "Time taken to find role by exact name")
    public Optional<Role> findRoleByNameExact(String name) {
        validateName(name);
        return roleRepository.findByNameExact(name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.update", description = "Time taken to update role")
    public Role updateRole(UUID id, String name, String description) {
        validateId(id);
        validateName(name);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrado com ID: " + id));

        // Verificar se nome já existe em outro role
        Optional<Role> existingRole = roleRepository.findByNameExact(name.trim());
        if (existingRole.isPresent() && !existingRole.get().getId().equals(id)) {
            throw new ConflictException("Nome já está em uso por outro role");
        }

        role.setName(name.trim());
        role.setDescription(description);
        role.updateTimestamp();

        return roleRepository.save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.delete", description = "Time taken to delete role")
    public boolean deleteRole(UUID id) {
        validateId(id);
        
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role não encontrado com ID: " + id);
        }

        roleRepository.deleteById(id);
        return true;
    }

    @Override
    public boolean existsRoleById(UUID id) {
        validateId(id);
        return roleRepository.existsById(id);
    }

    @Override
    public boolean existsRoleByName(String name) {
        validateName(name);
        return roleRepository.existsByName(name);
    }

    @Override
    @Timed(value = "roles.count", description = "Time taken to count roles")
    public long countRoles() {
        return roleRepository.count();
    }

    @Override
    public List<Role> searchRoles(String query) {
        return roleRepository.search(query);
    }

    @Override
    public List<Object> getRolePermissions(UUID roleId) {
        try {
            List<Object[]> results = roleRepository.getRolePermissionsDetails(roleId);
            return results.stream()
                    .map(row -> java.util.Map.of(
                        "id", row[0],
                        "action", row[1],
                        "resource", row[2]
                    ))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter permissões do role: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean roleHasPermission(UUID roleId, String action, String resource) {
        try {
            return roleRepository.roleHasPermissionByActionAndResource(roleId, action, resource);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar permissão do role: " + e.getMessage(), e);
        }
    }

    @Override
    public long countRolePermissions(UUID roleId) {
        try {
            return roleRepository.countPermissionsByRole(roleId);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar permissões do role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getRoleUsers(UUID roleId) {
        try {
            List<Object[]> results = roleRepository.getRoleUsersDetails(roleId);
            return results.stream()
                    .map(row -> java.util.Map.of(
                        "id", row[0],
                        "name", row[1],
                        "email", row[2],
                        "isActive", row[3]
                    ))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter usuários do role: " + e.getMessage(), e);
        }
    }

    @Override
    public long countRoleUsers(UUID roleId) {
        try {
            return roleRepository.countUsersByRole(roleId);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar usuários do role: " + e.getMessage(), e);
        }
    }

    // Métodos de validação privados
    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("ID do role não pode ser nulo");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome do role é obrigatório");
        }
        if (name.trim().length() < 2) {
            throw new ValidationException("Nome do role deve ter pelo menos 2 caracteres");
        }
    }
}