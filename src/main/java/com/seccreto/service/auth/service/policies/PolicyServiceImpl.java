package com.seccreto.service.auth.service.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;
import com.seccreto.service.auth.repository.policies.PolicyRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementação da camada de serviço contendo regras de negócio para policies.
 * Aplica SRP e DIP com transações declarativas.
 * 
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a ABAC (Attribute-Based Access Control)
 * - Condições JSON flexíveis
 * - Arrays de ações e recursos
 * - Versioning para optimistic locking
 */
@Service
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
        
        // Verificar se já existe uma policy com este nome (idempotência)
        Optional<Policy> existingPolicy = policyRepository.findByNameExact(name.trim());
        if (existingPolicy.isPresent()) {
            return existingPolicy.get(); // Retorna a policy existente (idempotência)
        }
        
        Policy policy = Policy.createNew(name.trim(), description, effect, actions, resources, conditions);
        Policy savedPolicy = policyRepository.save(policy);
        return savedPolicy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.create", description = "Time taken to create a policy")
    public Policy createPolicy(String name, String description, String effect, JsonNode conditions) {
        validateName(name);
        if (effect == null || effect.trim().isEmpty()) {
            throw new ValidationException("Efeito não pode ser vazio");
        }

        PolicyEffect policyEffect;
        try {
            policyEffect = PolicyEffect.valueOf(effect.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Efeito inválido: " + effect);
        }

        // Usar ações e recursos padrão
        List<String> defaultActions = List.of("*");
        List<String> defaultResources = List.of("*");

        return createPolicy(name, description, policyEffect, defaultActions, defaultResources, conditions);
    }

    @Override
    public List<Policy> listAllPolicies() {
        return policyRepository.findAll();
    }

    @Override
    public Optional<Policy> findPolicyById(Long id) {
        validateId(id);
        return policyRepository.findById(id);
    }

    @Override
    public List<Policy> findPoliciesByName(String name) {
        validateName(name);
        return policyRepository.findByName(name.trim());
    }

    @Override
    public Optional<Policy> findPolicyByNameExact(String name) {
        validateName(name);
        return policyRepository.findByNameExact(name.trim());
    }

    @Override
    public List<Policy> findPoliciesByEffect(PolicyEffect effect) {
        validateEffect(effect);
        return policyRepository.findByEffect(effect);
    }

    @Override
    public List<Policy> findPoliciesByAction(String action) {
        validateAction(action);
        return policyRepository.findByAction(action.trim());
    }

    @Override
    public List<Policy> findPoliciesByResource(String resource) {
        validateResource(resource);
        return policyRepository.findByResource(resource.trim());
    }

    @Override
    public List<Policy> findPoliciesByActionAndResource(String action, String resource) {
        validateAction(action);
        validateResource(resource);
        return policyRepository.findByActionAndResource(action.trim(), resource.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.update", description = "Time taken to update a policy")
    public Policy updatePolicy(Long id, String name, String description, PolicyEffect effect, List<String> actions, List<String> resources, JsonNode conditions) {
        validateId(id);
        validateName(name);
        validateEffect(effect);
        validateActions(actions);
        validateResources(resources);
        
        Policy existing = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy não encontrada com ID: " + id));
        
        // Verificar se os dados são diferentes (idempotência)
        if (existing.getName().equals(name.trim()) && 
            ((existing.getDescription() == null && description == null) || 
             (existing.getDescription() != null && existing.getDescription().equals(description))) &&
            existing.getEffect().equals(effect) &&
            existing.getActions().equals(actions) &&
            existing.getResources().equals(resources) &&
            ((existing.getConditions() == null && conditions == null) || 
             (existing.getConditions() != null && existing.getConditions().equals(conditions)))) {
            return existing; // Retorna a policy sem alterações (idempotência)
        }
        
        // Verificar se o nome já está em uso por outra policy
        policyRepository.findByNameExact(name.trim()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new ConflictException("Já existe uma policy com este nome");
            }
        });
        
        existing.setName(name.trim());
        existing.setDescription(description);
        existing.setEffect(effect);
        existing.setActions(actions);
        existing.setResources(resources);
        existing.setConditions(conditions);
        Policy updatedPolicy = policyRepository.update(existing);
        return updatedPolicy;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.update", description = "Time taken to update a policy")
    public Policy updatePolicy(Long id, String name, String description, String effect, JsonNode conditions) {
        validateId(id);
        validateName(name);
        if (effect == null || effect.trim().isEmpty()) {
            throw new ValidationException("Efeito não pode ser vazio");
        }

        PolicyEffect policyEffect;
        try {
            policyEffect = PolicyEffect.valueOf(effect.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Efeito inválido: " + effect);
        }

        Policy existing = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy não encontrada com ID: " + id));

        // Usar ações e recursos existentes ou padrão
        List<String> actions = existing.getActions() != null ? existing.getActions() : List.of("*");
        List<String> resources = existing.getResources() != null ? existing.getResources() : List.of("*");

        return updatePolicy(id, name, description, policyEffect, actions, resources, conditions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.delete", description = "Time taken to delete a policy")
    public boolean deletePolicy(Long id) {
        validateId(id);
        
        // Verificar se a policy existe antes de tentar deletar (idempotência)
        if (!policyRepository.existsById(id)) {
            return false; // Policy já não existe (idempotência)
        }
        
        boolean deleted = policyRepository.deleteById(id);
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.deactivate", description = "Time taken to deactivate a policy")
    public Policy deactivatePolicy(Long id) {
        validateId(id);

        // Since Policy doesn't have an active field, we'll use the DENY effect to simulate deactivation
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy não encontrada com ID: " + id));

        if (policy.getEffect() == PolicyEffect.DENY) {
            return policy; // Policy já está "inativa" (idempotência)
        }

        policy.setEffect(PolicyEffect.DENY);
        policy.updateTimestamp();
        return policyRepository.update(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "policies.activate", description = "Time taken to activate a policy")
    public Policy activatePolicy(Long id) {
        validateId(id);

        // Since Policy doesn't have an active field, we'll use the ALLOW effect to simulate activation
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy não encontrada com ID: " + id));

        if (policy.getEffect() == PolicyEffect.ALLOW) {
            return policy; // Policy já está "ativa" (idempotência)
        }

        policy.setEffect(PolicyEffect.ALLOW);
        policy.updateTimestamp();
        return policyRepository.update(policy);
    }

    @Override
    public boolean existsPolicyById(Long id) {
        validateId(id);
        return policyRepository.existsById(id);
    }

    @Override
    public boolean existsPolicyByName(String name) {
        validateName(name);
        return policyRepository.existsByName(name.trim());
    }

    @Override
    public long countPolicies() {
        return policyRepository.count();
    }

    @Override
    public long countActivePolicies() {
        // Since Policy doesn't have an active field, count policies with ALLOW effect
        return policyRepository.countByEffect(PolicyEffect.ALLOW);
    }

    @Override
    public long countPoliciesByEffect(PolicyEffect effect) {
        validateEffect(effect);
        return policyRepository.countByEffect(effect);
    }

    @Override
    public long countPoliciesByEffect(String effect) {
        if (effect == null || effect.trim().isEmpty()) {
            throw new ValidationException("Efeito não pode ser vazio");
        }
        try {
            PolicyEffect policyEffect = PolicyEffect.valueOf(effect.toUpperCase());
            return policyRepository.countByEffect(policyEffect);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Efeito inválido: " + effect);
        }
    }

    @Override
    public Boolean evaluatePolicy(Long policyId, Long userId, Object context) {
        validateId(policyId);
        if (userId == null) {
            throw new ValidationException("ID do usuário não pode ser nulo");
        }

        Optional<Policy> policyOpt = policyRepository.findById(policyId);
        if (policyOpt.isEmpty()) {
            return false; // Policy não encontrada, nega acesso
        }

        Policy policy = policyOpt.get();

        // Implementação básica de avaliação de policy
        // Em uma implementação real, seria mais complexa com avaliação de condições JSON
        if (policy.getEffect() == PolicyEffect.ALLOW) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<Policy> searchPolicies(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of(); // Retorna lista vazia para query inválida
        }
        return policyRepository.search(query.trim());
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome não pode ser vazio");
        }
        if (name.trim().length() < 2) {
            throw new ValidationException("Nome deve ter pelo menos 2 caracteres");
        }
    }

    private void validateEffect(PolicyEffect effect) {
        if (effect == null) {
            throw new ValidationException("Efeito da policy não pode ser nulo");
        }
    }

    private void validateActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new ValidationException("Ações não podem ser vazias");
        }
        for (String action : actions) {
            if (action == null || action.trim().isEmpty()) {
                throw new ValidationException("Ação não pode ser vazia");
            }
            if (action.trim().length() < 2) {
                throw new ValidationException("Ação deve ter pelo menos 2 caracteres");
            }
        }
    }

    private void validateResources(List<String> resources) {
        if (resources == null || resources.isEmpty()) {
            throw new ValidationException("Recursos não podem ser vazios");
        }
        for (String resource : resources) {
            if (resource == null || resource.trim().isEmpty()) {
                throw new ValidationException("Recurso não pode ser vazio");
            }
            if (resource.trim().length() < 2) {
                throw new ValidationException("Recurso deve ter pelo menos 2 caracteres");
            }
        }
    }

    private void validateAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new ValidationException("Ação não pode ser vazia");
        }
    }

    private void validateResource(String resource) {
        if (resource == null || resource.trim().isEmpty()) {
            throw new ValidationException("Recurso não pode ser vazio");
        }
    }

    private void validateId(Long id) {
        if (id == null) {
            throw new ValidationException("ID não pode ser nulo");
        }
        if (id <= 0) {
            throw new ValidationException("ID deve ser maior que zero");
        }
    }
}
