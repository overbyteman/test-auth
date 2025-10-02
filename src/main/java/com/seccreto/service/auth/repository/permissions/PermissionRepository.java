package com.seccreto.service.auth.repository.permissions;

import com.seccreto.service.auth.model.permissions.Permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração de repositório para a entidade Permission, permitindo trocar implementação (in-memory, JPA, etc.).
 * Baseado na migração V5.
 */
public interface PermissionRepository {
    Permission save(Permission permission);
    Optional<Permission> findById(UUID id);
    List<Permission> findAll();
    List<Permission> findByAction(String action);
    List<Permission> findByResource(String resource);
    Optional<Permission> findByActionAndResource(String action, String resource);
    Permission update(Permission permission);
    boolean deleteById(UUID id);
    boolean existsById(UUID id);
    boolean existsByActionAndResource(String action, String resource);
    long count();
    void clear();
    
    // Métodos adicionais para controllers
    List<Permission> search(String query);
}