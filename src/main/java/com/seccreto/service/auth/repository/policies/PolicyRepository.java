package com.seccreto.service.auth.repository.policies;

import com.seccreto.service.auth.model.policies.Policy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração de repositório para a entidade Policy, permitindo trocar implementação (in-memory, JPA, etc.).
 * Baseado na migração V8.
 */
public interface PolicyRepository {
    Policy save(Policy policy);
    Optional<Policy> findById(UUID id);
    List<Policy> findAll();
    List<Policy> findByName(String name);
    Optional<Policy> findByNameExact(String name);
    List<Policy> findByEffect(String effect);
    List<Policy> findByEffectAndConditions(String effect, String conditions);
    Policy update(Policy policy);
    boolean deleteById(UUID id);
    boolean existsById(UUID id);
    boolean existsByName(String name);
    long count();
    void clear();
    
    // Métodos adicionais para controllers
    List<Policy> search(String query);
}