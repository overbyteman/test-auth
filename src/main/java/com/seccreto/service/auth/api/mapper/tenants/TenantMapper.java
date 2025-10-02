package com.seccreto.service.auth.api.mapper.tenants;

import com.seccreto.service.auth.api.dto.tenants.TenantRequest;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.model.tenants.Tenant;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversão entre DTOs e entidades de Tenant.
 * Implementa o padrão de mapeamento estático para performance.
 */
public final class TenantMapper {
    private TenantMapper() {}

    /**
     * Converte TenantRequest para entidade Tenant.
     */
    public static Tenant toEntity(TenantRequest request) {
        if (request == null) {
            return null;
        }
        
        return Tenant.builder()
                .name(request.getName())
                .config(request.getConfig())
                .build();
    }

    /**
     * Converte entidade Tenant para TenantResponse.
     */
    public static TenantResponse toResponse(Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .config(tenant.getConfig())
                .build();
    }

    /**
     * Converte lista de entidades Tenant para lista de TenantResponse.
     */
    public static List<TenantResponse> toResponseList(List<Tenant> tenants) {
        if (tenants == null) {
            return null;
        }
        
        return tenants.stream()
                .map(TenantMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza entidade Tenant com dados do TenantRequest.
     */
    public static void updateEntity(Tenant tenant, TenantRequest request) {
        if (tenant == null || request == null) {
            return;
        }
        
        tenant.setName(request.getName());
        tenant.setConfig(request.getConfig());
    }
}
