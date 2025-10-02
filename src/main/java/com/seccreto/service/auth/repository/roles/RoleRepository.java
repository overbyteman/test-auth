package com.seccreto.service.auth.repository.roles;

import com.seccreto.service.auth.model.roles.Role;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração de repositório para a entidade Role, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findById(UUID id);
    List<Role> findAll();
    List<Role> findByName(String name);
    Optional<Role> findByNameExact(String name);
    Role update(Role role);
    boolean deleteById(UUID id);
    boolean existsById(UUID id);
    boolean existsByName(String name);
    long count();
    void clear();
    
    // Métodos adicionais para controllers
    List<Role> search(String query);
    Map<String, Long> getRoleDistribution();
}