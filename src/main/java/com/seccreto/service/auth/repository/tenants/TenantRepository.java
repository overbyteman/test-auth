package com.seccreto.service.auth.repository.tenants;

import com.seccreto.service.auth.model.tenants.Tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração de repositório para a entidade Tenant, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(UUID id);
    List<Tenant> findAll();
    List<Tenant> findByName(String name);
    Optional<Tenant> findByNameExact(String name);
    Tenant update(Tenant tenant);
    boolean deleteById(UUID id);
    boolean existsById(UUID id);
    boolean existsByName(String name);
    long count();
    void clear();
    
    // Métodos adicionais para controllers
    long countCreatedToday();
    long countCreatedThisWeek();
    long countCreatedThisMonth();
    long countInPeriod(String startDate, String endDate);
    List<Tenant> search(String query);
}