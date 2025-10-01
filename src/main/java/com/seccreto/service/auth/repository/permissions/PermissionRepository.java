package com.seccreto.service.auth.repository.permissions;

import com.seccreto.service.auth.model.permissions.Permission;

import java.util.List;
import java.util.Optional;

/**
 * Abstração de repositório para a entidade Permission, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface PermissionRepository {
    Permission save(Permission permission);
    Optional<Permission> findById(Long id);
    List<Permission> findAll();
    List<Permission> findByAction(String action);
    List<Permission> findByResource(String resource);
    List<Permission> findByActionAndResource(String action, String resource);
    Optional<Permission> findByActionAndResourceExact(String action, String resource);
    Permission update(Permission permission);
    boolean deleteById(Long id);
    boolean existsById(Long id);
    boolean existsByActionAndResource(String action, String resource);
    long count();
    void clear();
    
    // Métodos adicionais para controllers
    List<Permission> search(String query);
}
