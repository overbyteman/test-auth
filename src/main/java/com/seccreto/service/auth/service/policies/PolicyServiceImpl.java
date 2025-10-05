package com.seccreto.service.auth.service.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.repository.policies.PolicyRepository;
import com.seccreto.service.auth.repository.tenants.TenantRepository;
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
 * Implementação da camada de serviço contendo regras de negócio para policies.
 * Aplica SRP e DIP com transações declarativas.
 * Baseado na migração V8.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final TenantRepository tenantRepository;

    public PolicyServiceImpl(PolicyRepository policyRepository, TenantRepository tenantRepository) {
        this.policyRepository = policyRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.create", description = "Time taken to create a policy")
    public Policy createPolicy(UUID tenantId, String code, String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions) {
        validateTenantId(tenantId);
        validateCode(code);
        validateName(name);
        validateEffect(effect);
        validateActions(actions);
        validateResources(resources);
        validateConditions(conditions);

        Tenant tenant = findTenant(tenantId);

        policyRepository.findByTenantIdAndCode(tenantId, code.trim())
                .ifPresent(existing -> { throw new ConflictException("Code da policy já está em uso para este tenant"); });

        policyRepository.findByTenantIdAndName(tenantId, name.trim())
                .ifPresent(existing -> { throw new ConflictException("Nome da policy já está em uso para este tenant"); });

        Policy policy = Policy.createNew(tenant, code.trim(), name.trim(), description, effect, actions, resources, conditions);
        return policyRepository.save(policy);
    }

    @Override
    @Timed(value = "policies.list", description = "Time taken to list policies")
    public List<Policy> listPolicies(UUID tenantId) {
        validateTenantId(tenantId);
        return policyRepository.findByTenantId(tenantId);
    }

    @Override
    @Timed(value = "policies.find", description = "Time taken to find policy by id")
    public Optional<Policy> findPolicyById(UUID tenantId, UUID id) {
        validateTenantId(tenantId);
        validateId(id);
        return policyRepository.findById(id)
                .filter(policy -> policy.getTenant() != null && tenantId.equals(policy.getTenant().getId()));
    }

    @Override
    @Timed(value = "policies.find", description = "Time taken to find policies by name")
    public List<Policy> findPoliciesByName(UUID tenantId, String name) {
        validateTenantId(tenantId);
        validateName(name);
        return policyRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, name.trim());
    }

    @Override
    public Optional<Policy> findPolicyByCode(UUID tenantId, String code) {
        validateTenantId(tenantId);
        validateCode(code);
        return policyRepository.findByTenantIdAndCode(tenantId, code.trim());
    }

    @Override
    @Timed(value = "policies.find", description = "Time taken to find policies by effect")
    public List<Policy> findPoliciesByEffect(UUID tenantId, String effect) {
        validateTenantId(tenantId);
        validateEffectString(effect);
        PolicyEffect policyEffect = PolicyEffect.valueOf(effect.toUpperCase());
        return policyRepository.findByTenantIdAndEffect(tenantId, policyEffect);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.update", description = "Time taken to update policy")
    public Policy updatePolicy(UUID tenantId, UUID id, String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions) {
        validateTenantId(tenantId);
        validateId(id);
        validateName(name);
        validateEffect(effect);
        validateActions(actions);
        validateResources(resources);
        validateConditions(conditions);

        Policy policy = requirePolicy(tenantId, id);

        policyRepository.findByTenantIdAndName(tenantId, name.trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new ConflictException("Nome da policy já está em uso para este tenant"); });

        policy.setName(name.trim());
        policy.setDescription(description);
        policy.setEffect(effect);
        policy.setActions(actions);
        policy.setResources(resources);
        policy.setConditions(conditions);

        return policyRepository.save(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.delete", description = "Time taken to delete policy")
    public boolean deletePolicy(UUID tenantId, UUID id) {
        validateTenantId(tenantId);
        validateId(id);

        Policy policy = requirePolicy(tenantId, id);
        policyRepository.delete(policy);
        return true;
    }

    @Override
    public boolean existsPolicyById(UUID tenantId, UUID id) {
        validateTenantId(tenantId);
        validateId(id);
        return policyRepository.findById(id)
                .map(policy -> policy.getTenant() != null && tenantId.equals(policy.getTenant().getId()))
                .orElse(false);
    }

    @Override
    public boolean existsPolicyByCode(UUID tenantId, String code) {
        validateTenantId(tenantId);
        validateCode(code);
        return policyRepository.existsByTenantIdAndCode(tenantId, code.trim());
    }

    @Override
    @Timed(value = "policies.count", description = "Time taken to count policies")
    public long countPolicies(UUID tenantId) {
        validateTenantId(tenantId);
        return policyRepository.countByTenantId(tenantId);
    }

    @Override
    public List<Policy> searchPolicies(UUID tenantId, String query) {
        validateTenantId(tenantId);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return policyRepository.search(tenantId, query.trim());
    }

    @Override
    public boolean isPolicyValid(Policy policy) {
        if (policy == null) {
            return false;
        }
        
        try {
            validateName(policy.getName());
            validateEffect(policy.getEffect());
            validateActions(policy.getActions());
            validateResources(policy.getResources());
            validateConditions(policy.getConditions());
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    @Override
    public boolean policyMatchesConditions(Policy policy, String context) {
        if (policy == null || context == null) {
            return false;
        }
        
        // Implementação simplificada - em produção seria mais complexa
        JsonNode conditions = policy.getConditions();
        if (conditions == null || conditions.isNull()) {
            return true; // Policy sem condições sempre aplica
        }
        
        // Verificação básica de contexto
        String conditionsStr = conditions.toString();
        return context.contains(conditionsStr) || conditionsStr.contains(context);
    }

    @Override
    public String evaluatePolicyEffect(Policy policy, String context) {
        if (policy == null) {
            return "DENY"; // Default deny
        }
        
        if (policyMatchesConditions(policy, context)) {
            return policy.getEffect().name();
        }
        
        return "DENY"; // Default deny se não aplicar
    }

    @Override
    public List<Policy> findPoliciesByEffectAndConditions(UUID tenantId, String effect, String conditions) {
        validateTenantId(tenantId);
        validateEffectString(effect);
        validateConditionsString(conditions);
        PolicyEffect policyEffect = PolicyEffect.valueOf(effect.toUpperCase());
        return policyRepository.findByEffectAndConditions(tenantId, policyEffect, conditions);
    }

    // Métodos de validação privados
    private void validateTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new ValidationException("TenantId da policy não pode ser nulo");
        }
    }

    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("ID da policy não pode ser nulo");
        }
    }

    private void validateCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new ValidationException("Código da policy é obrigatório");
        }
        if (code.trim().length() < 2) {
            throw new ValidationException("Código da policy deve ter pelo menos 2 caracteres");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome da policy é obrigatório");
        }
        if (name.trim().length() < 2) {
            throw new ValidationException("Nome da policy deve ter pelo menos 2 caracteres");
        }
    }

    private void validateEffect(PolicyEffect effect) {
        if (effect == null) {
            throw new ValidationException("Efeito da policy é obrigatório");
        }
    }

    private void validateActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new ValidationException("Ações da policy são obrigatórias");
        }
    }

    private void validateResources(List<String> resources) {
        if (resources == null || resources.isEmpty()) {
            throw new ValidationException("Recursos da policy são obrigatórios");
        }
    }

    private void validateConditions(JsonNode conditions) {
        if (conditions == null) {
            throw new ValidationException("Condições da policy são obrigatórias");
        }
    }

    private void validateEffectString(String effect) {
        if (effect == null || effect.trim().isEmpty()) {
            throw new ValidationException("Efeito da policy é obrigatório");
        }
        if (!effect.equals("allow") && !effect.equals("deny")) {
            throw new ValidationException("Efeito da policy deve ser allow ou deny");
        }
    }

    private void validateConditionsString(String conditions) {
        if (conditions == null || conditions.trim().isEmpty()) {
            throw new ValidationException("Condições da policy são obrigatórias");
        }
        if (conditions.trim().length() < 2) {
            throw new ValidationException("Condições da policy devem ter pelo menos 2 caracteres");
        }
    }

    private Tenant findTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + tenantId));
    }

    private Policy requirePolicy(UUID tenantId, UUID policyId) {
        return policyRepository.findById(policyId)
                .filter(policy -> policy.getTenant() != null && tenantId.equals(policy.getTenant().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy não encontrada para o tenant informado (tenantId=" + tenantId + ", policyId=" + policyId + ")"));
    }
}