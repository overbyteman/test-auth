package com.seccreto.service.auth.service.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;

import java.util.List;
import java.util.Optional;

/**
 * Abstração da camada de serviço para operações de policy.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 */
public interface PolicyService {
    Policy createPolicy(String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions);
    Policy createPolicy(String name, String description, String effect, JsonNode conditions);
    List<Policy> listAllPolicies();
    Optional<Policy> findPolicyById(Long id);
    List<Policy> findPoliciesByName(String name);
    Optional<Policy> findPolicyByNameExact(String name);
    List<Policy> findPoliciesByEffect(PolicyEffect effect);
    List<Policy> findPoliciesByAction(String action);
    List<Policy> findPoliciesByResource(String resource);
    List<Policy> findPoliciesByActionAndResource(String action, String resource);
    Policy updatePolicy(Long id, String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions);
    Policy updatePolicy(Long id, String name, String description, String effect, JsonNode conditions);
    boolean deletePolicy(Long id);
    boolean existsPolicyById(Long id);
    boolean existsPolicyByName(String name);
    long countPolicies();
    long countPoliciesByEffect(PolicyEffect effect);
    
    // Métodos adicionais para controllers
    Policy deactivatePolicy(Long id);
    Policy activatePolicy(Long id);
    long countActivePolicies();
    long countPoliciesByEffect(String effect);
    List<Policy> searchPolicies(String query);
    Boolean evaluatePolicy(Long policyId, Long userId, Object context);
}
