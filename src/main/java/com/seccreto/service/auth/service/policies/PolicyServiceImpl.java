package com.seccreto.service.auth.service.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;
import com.seccreto.service.auth.repository.policies.PolicyRepository;
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

    public PolicyServiceImpl(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.create", description = "Time taken to create a policy")
    public Policy createPolicy(String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions) {
        validateName(name);
        validateEffect(effect);
        validateActions(actions);
        validateResources(resources);
        validateConditions(conditions);

        // Verificar se já existe uma policy com este nome (idempotência)
        Optional<Policy> existingPolicy = policyRepository.findByNameExact(name.trim());
        if (existingPolicy.isPresent()) {
            return existingPolicy.get(); // Retorna a policy existente (idempotência)
        }

        Policy policy = Policy.createNew(name.trim(), description, effect, actions, resources, conditions);
        return policyRepository.save(policy);
    }

    @Override
    @Timed(value = "policies.list", description = "Time taken to list policies")
    public List<Policy> listAllPolicies() {
        return policyRepository.findAll();
    }

    @Override
    @Timed(value = "policies.find", description = "Time taken to find policy by id")
    public Optional<Policy> findPolicyById(UUID id) {
        validateId(id);
        return policyRepository.findById(id);
    }

    @Override
    @Timed(value = "policies.find", description = "Time taken to find policies by name")
    public List<Policy> findPoliciesByName(String name) {
        validateName(name);
        Optional<Policy> policy = policyRepository.findByName(name);
        return policy.map(List::of).orElse(List.of());
    }

    @Override
    @Timed(value = "policies.find", description = "Time taken to find policy by exact name")
    public Optional<Policy> findPolicyByNameExact(String name) {
        validateName(name);
        return policyRepository.findByNameExact(name);
    }

    @Override
    @Timed(value = "policies.find", description = "Time taken to find policies by effect")
    public List<Policy> findPoliciesByEffect(String effect) {
        validateEffectString(effect);
        PolicyEffect policyEffect = PolicyEffect.valueOf(effect.toUpperCase());
        return policyRepository.findByEffect(policyEffect);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.update", description = "Time taken to update policy")
    public Policy updatePolicy(UUID id, String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions) {
        validateId(id);
        validateName(name);
        validateEffect(effect);
        validateActions(actions);
        validateResources(resources);
        validateConditions(conditions);

        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy não encontrada com ID: " + id));

        // Verificar se nome já existe em outra policy
        Optional<Policy> existingPolicy = policyRepository.findByNameExact(name.trim());
        if (existingPolicy.isPresent() && !existingPolicy.get().getId().equals(id)) {
            throw new ConflictException("Nome já está em uso por outra policy");
        }

        policy.setName(name.trim());
        policy.setDescription(description);
        policy.setEffect(effect);
        policy.setActions(actions);
        policy.setResources(resources);
        policy.setConditions(conditions);
        policy.updateTimestamp();

        return policyRepository.save(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.delete", description = "Time taken to delete policy")
    public boolean deletePolicy(UUID id) {
        validateId(id);
        
        if (!policyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Policy não encontrada com ID: " + id);
        }

        policyRepository.deleteById(id);
        return true;
    }

    @Override
    public boolean existsPolicyById(UUID id) {
        validateId(id);
        return policyRepository.existsById(id);
    }

    @Override
    public boolean existsPolicyByName(String name) {
        validateName(name);
        return policyRepository.existsByName(name);
    }

    @Override
    @Timed(value = "policies.count", description = "Time taken to count policies")
    public long countPolicies() {
        return policyRepository.count();
    }

    @Override
    public List<Policy> searchPolicies(String query) {
        return policyRepository.search(query);
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
    public List<Policy> findPoliciesByEffectAndConditions(String effect, String conditions) {
        validateEffectString(effect);
        validateConditionsString(conditions);
        PolicyEffect policyEffect = PolicyEffect.valueOf(effect.toUpperCase());
        return policyRepository.findByEffectAndConditions(policyEffect, conditions);
    }

    // Métodos de validação privados
    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("ID da policy não pode ser nulo");
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
}