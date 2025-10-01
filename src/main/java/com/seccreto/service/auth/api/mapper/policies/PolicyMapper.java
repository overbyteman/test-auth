package com.seccreto.service.auth.api.mapper.policies;

import com.seccreto.service.auth.api.dto.policies.PolicyRequest;
import com.seccreto.service.auth.api.dto.policies.PolicyResponse;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversão entre DTOs e entidades de Policy.
 * Implementa o padrão de mapeamento estático para performance.
 */
public final class PolicyMapper {
    private PolicyMapper() {}

    /**
     * Converte PolicyRequest para entidade Policy.
     */
    public static Policy toEntity(PolicyRequest request) {
        if (request == null) {
            return null;
        }
        
        PolicyEffect effect = null;
        if (request.getEffect() != null) {
            try {
                effect = PolicyEffect.valueOf(request.getEffect().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Efeito inválido: " + request.getEffect());
            }
        }
        
        return Policy.builder()
                .name(request.getName())
                .description(request.getDescription())
                .effect(effect)
                .actions(request.getActions())
                .resources(request.getResources())
                .conditions(request.getConditions())
                .build();
    }

    /**
     * Converte entidade Policy para PolicyResponse.
     */
    public static PolicyResponse toResponse(Policy policy) {
        if (policy == null) {
            return null;
        }
        
        return PolicyResponse.builder()
                .id(policy.getId())
                .name(policy.getName())
                .description(policy.getDescription())
                .effect(policy.getEffect() != null ? policy.getEffect().name() : null)
                .actions(policy.getActions())
                .resources(policy.getResources())
                .conditions(policy.getConditions())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .version(policy.getVersion())
                .isAllow(policy.isAllow())
                .isDeny(policy.isDeny())
                .build();
    }

    /**
     * Converte lista de entidades Policy para lista de PolicyResponse.
     */
    public static List<PolicyResponse> toResponseList(List<Policy> policies) {
        if (policies == null) {
            return null;
        }
        
        return policies.stream()
                .map(PolicyMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza entidade Policy com dados do PolicyRequest.
     */
    public static void updateEntity(Policy policy, PolicyRequest request) {
        if (policy == null || request == null) {
            return;
        }
        
        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        
        if (request.getEffect() != null) {
            try {
                policy.setEffect(PolicyEffect.valueOf(request.getEffect().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Efeito inválido: " + request.getEffect());
            }
        }
        
        policy.setActions(request.getActions());
        policy.setResources(request.getResources());
        policy.setConditions(request.getConditions());
    }
}
