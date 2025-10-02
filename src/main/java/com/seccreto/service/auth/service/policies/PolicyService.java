package com.seccreto.service.auth.service.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de policy.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 * Baseado na migração V8.
 */
public interface PolicyService {
    
    // Operações básicas CRUD
    Policy createPolicy(String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions);
    List<Policy> listAllPolicies();
    Optional<Policy> findPolicyById(UUID id);
    List<Policy> findPoliciesByName(String name);
    Optional<Policy> findPolicyByNameExact(String name);
    List<Policy> findPoliciesByEffect(String effect);
    Policy updatePolicy(UUID id, String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions);
    boolean deletePolicy(UUID id);
    boolean existsPolicyById(UUID id);
    boolean existsPolicyByName(String name);
    long countPolicies();
    
    // Operações de busca
    List<Policy> searchPolicies(String query);
    
    // Operações de validação
    boolean isPolicyValid(Policy policy);
    boolean policyMatchesConditions(Policy policy, String context);
    
    // Operações de efeito
    String evaluatePolicyEffect(Policy policy, String context);
    List<Policy> findPoliciesByEffectAndConditions(String effect, String conditions);
}