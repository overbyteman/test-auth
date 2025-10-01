package com.seccreto.service.auth.repository.policies;

import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;

import java.util.List;
import java.util.Optional;

/**
 * Abstração de repositório para a entidade Policy, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface PolicyRepository {
    Policy save(Policy policy);
    Optional<Policy> findById(Long id);
    List<Policy> findAll();
    List<Policy> findByName(String name);
    Optional<Policy> findByNameExact(String name);
    List<Policy> findByEffect(PolicyEffect effect);
    List<Policy> findByAction(String action);
    List<Policy> findByResource(String resource);
    List<Policy> findByActionAndResource(String action, String resource);
    Policy update(Policy policy);
    boolean deleteById(Long id);
    boolean existsById(Long id);
    boolean existsByName(String name);
    long count();
    long countByEffect(PolicyEffect effect);
    void clear();
    
    // Métodos adicionais para controllers
    List<Policy> search(String query);
}
