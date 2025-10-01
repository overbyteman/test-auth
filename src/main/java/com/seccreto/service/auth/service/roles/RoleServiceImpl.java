package com.seccreto.service.auth.service.roles;

import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.repository.roles.RoleRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação da camada de serviço contendo regras de negócio para roles.
 * Aplica SRP e DIP com transações declarativas.
 * 
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a RBAC (Role-Based Access Control)
 * - Versioning para optimistic locking
 */
@Service
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
        Role savedRole = roleRepository.save(role);
        return savedRole;
    }

    @Override
    public List<Role> listAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Optional<Role> findRoleById(Long id) {
        validateId(id);
        return roleRepository.findById(id);
    }

    @Override
    public List<Role> findRolesByName(String name) {
        validateName(name);
        return roleRepository.findByName(name.trim());
    }

    @Override
    public Optional<Role> findRoleByNameExact(String name) {
        validateName(name);
        return roleRepository.findByNameExact(name.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.update", description = "Time taken to update a role")
    public Role updateRole(Long id, String name, String description) {
        validateId(id);
        validateName(name);
        
        Role existing = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrado com ID: " + id));
        
        // Verificar se os dados são diferentes (idempotência)
        if (existing.getName().equals(name.trim()) && 
            ((existing.getDescription() == null && description == null) || 
             (existing.getDescription() != null && existing.getDescription().equals(description)))) {
            return existing; // Retorna o role sem alterações (idempotência)
        }
        
        // Verificar se o nome já está em uso por outro role
        roleRepository.findByNameExact(name.trim()).ifPresent(r -> {
            if (!r.getId().equals(id)) {
                throw new ConflictException("Já existe um role com este nome");
            }
        });
        
        existing.setName(name.trim());
        existing.setDescription(description);
        Role updatedRole = roleRepository.update(existing);
        return updatedRole;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.delete", description = "Time taken to delete a role")
    public boolean deleteRole(Long id) {
        validateId(id);
        
        // Verificar se o role existe antes de tentar deletar (idempotência)
        if (!roleRepository.existsById(id)) {
            return false; // Role já não existe (idempotência)
        }
        
        boolean deleted = roleRepository.deleteById(id);
        return deleted;
    }

    @Override
    public boolean existsRoleById(Long id) {
        validateId(id);
        return roleRepository.existsById(id);
    }

    @Override
    public boolean existsRoleByName(String name) {
        validateName(name);
        return roleRepository.existsByName(name.trim());
    }

    @Override
    public long countRoles() {
        return roleRepository.count();
    }

    @Override
    public Map<String, Long> getRoleDistribution() {
        return roleRepository.getRoleDistribution();
    }

    @Override
    public List<Role> searchRoles(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of(); // Retorna lista vazia para query inválida
        }
        return roleRepository.search(query.trim());
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome não pode ser vazio");
        }
        if (name.trim().length() < 2) {
            throw new ValidationException("Nome deve ter pelo menos 2 caracteres");
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
