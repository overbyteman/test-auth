package com.seccreto.service.auth.service.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.policies.PolicyResponse;
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
    Policy createPolicy(UUID tenantId, String code, String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions);
    List<Policy> listPolicies(UUID tenantId);
    Optional<Policy> findPolicyById(UUID tenantId, UUID id);
    Optional<Policy> findPolicyByCode(UUID tenantId, String code);
    List<Policy> findPoliciesByName(UUID tenantId, String name);
    List<Policy> findPoliciesByEffect(UUID tenantId, String effect);
    Policy updatePolicy(UUID tenantId, UUID id, String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions);
    boolean deletePolicy(UUID tenantId, UUID id);
    boolean existsPolicyById(UUID tenantId, UUID id);
    boolean existsPolicyByCode(UUID tenantId, String code);
    long countPolicies(UUID tenantId);
    
    // Operações de busca
    List<Policy> searchPolicies(UUID tenantId, String query);
    Pagination<PolicyResponse> searchPolicies(UUID tenantId, SearchQuery searchQuery);
    
    // Operações de validação
    boolean isPolicyValid(Policy policy);
    boolean policyMatchesConditions(Policy policy, String context);
    
    // Operações de efeito
    String evaluatePolicyEffect(Policy policy, String context);
    List<Policy> findPoliciesByEffectAndConditions(UUID tenantId, String effect, String conditions);
}