package com.seccreto.service.auth.service.tenants;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.tenants.Tenant;

import java.util.List;
import java.util.Optional;

/**
 * Abstração da camada de serviço para operações de tenant.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 */
public interface TenantService {
    Tenant createTenant(String name, JsonNode config);
    Tenant createTenant(String name, String description, String domain);
    List<Tenant> listAllTenants();
    Optional<Tenant> findTenantById(Long id);
    List<Tenant> findTenantsByName(String name);
    Optional<Tenant> findTenantByNameExact(String name);
    Tenant updateTenant(Long id, String name, JsonNode config);
    Tenant updateTenant(Long id, String name, String description, String domain);
    boolean deleteTenant(Long id);
    boolean existsTenantById(Long id);
    boolean existsTenantByName(String name);
    long countTenants();
    
    // Métodos adicionais para controllers
    Tenant deactivateTenant(Long id);
    Tenant activateTenant(Long id);
    long countActiveTenants();
    long countInactiveTenants();
    long countTenantsCreatedToday();
    long countTenantsCreatedThisWeek();
    long countTenantsCreatedThisMonth();
    long countTenantsInPeriod(String startDate, String endDate);
    List<Tenant> searchTenants(String query);
}
