package com.seccreto.service.auth.service.tenants;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.model.tenants.Tenant;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de tenant.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 * Baseado na migração V2.
 */
public interface TenantService {
    
    // Operações básicas CRUD
    Tenant createTenant(String name, JsonNode config, UUID landlordId);
    List<Tenant> listAllTenants();
    Optional<Tenant> findTenantById(UUID id);
    List<Tenant> findTenantsByName(String name);
    Optional<Tenant> findTenantByNameExact(String name);
    List<Tenant> findTenantsByLandlordId(UUID landlordId);
    long countTenantsByLandlordId(UUID landlordId);
    Tenant updateTenant(UUID id, String name, JsonNode config, UUID landlordId);
    boolean deleteTenant(UUID id);
    boolean existsTenantById(UUID id);
    boolean existsTenantByName(String name);
    long countTenants();
    
    // Operações de configuração
    Tenant updateTenantConfig(UUID id, JsonNode config);
    JsonNode getTenantConfig(UUID id);
    
    // Operações de busca e filtros
    List<Tenant> searchTenants(String query);
    Pagination<TenantResponse> searchTenants(SearchQuery searchQuery);
    
    // Métricas e estatísticas
    long countTenantsCreatedToday();
    long countTenantsCreatedThisWeek();
    long countTenantsCreatedThisMonth();
    long countTenantsInPeriod(LocalDate startDate, LocalDate endDate);
    
    // Operações de usuários por tenant
    List<Object> getTenantUsers(UUID tenantId);
    long countTenantUsers(UUID tenantId);
    List<Object> getTenantUsersWithRoles(UUID tenantId);
}