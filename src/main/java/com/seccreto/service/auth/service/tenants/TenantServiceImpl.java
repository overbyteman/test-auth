package com.seccreto.service.auth.service.tenants;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.repository.tenants.TenantRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.usage.UsageService;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação da camada de serviço contendo regras de negócio para tenants.
 * Aplica SRP e DIP com transações declarativas.
 * Baseado na migração V2.
 *
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a UUIDs
 * - Configuração JSON flexível
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UsageService usageService;
    public TenantServiceImpl(TenantRepository tenantRepository, 
                            UsageService usageService) {
        this.tenantRepository = tenantRepository;
        this.usageService = usageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "tenants.create", description = "Time taken to create a tenant")
    public Tenant createTenant(String name, JsonNode config) {
        validateName(name);

        // Verificar se já existe um tenant com este nome (idempotência)
    Optional<Tenant> existingTenant = tenantRepository.findByName(name.trim());
        if (existingTenant.isPresent()) {
            return existingTenant.get(); // Retorna o tenant existente (idempotência)
        }

        Tenant tenant = Tenant.createNew(name.trim(), config);
        return tenantRepository.save(tenant);
    }

    @Override
    @Timed(value = "tenants.list", description = "Time taken to list tenants")
    public List<Tenant> listAllTenants() {
        return tenantRepository.findAll();
    }

    @Override
    @Timed(value = "tenants.find", description = "Time taken to find tenant by id")
    public Optional<Tenant> findTenantById(UUID id) {
        validateId(id);
        return tenantRepository.findById(id);
    }

    @Override
    @Timed(value = "tenants.find", description = "Time taken to find tenants by name")
    public List<Tenant> findTenantsByName(String name) {
        validateName(name);
        Optional<Tenant> tenant = tenantRepository.findByName(name);
        return tenant.map(List::of).orElse(List.of());
    }

    @Override
    @Timed(value = "tenants.find", description = "Time taken to find tenant by exact name")
    public Optional<Tenant> findTenantByNameExact(String name) {
        validateName(name);
    return tenantRepository.findByName(name.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "tenants.update", description = "Time taken to update tenant")
    public Tenant updateTenant(UUID id, String name, JsonNode config) {
        validateId(id);
        validateName(name);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + id));

        // Verificar se nome já existe em outro tenant
    Optional<Tenant> existingTenant = tenantRepository.findByName(name.trim());
        if (existingTenant.isPresent() && !existingTenant.get().getId().equals(id)) {
            throw new ConflictException("Nome já está em uso por outro tenant");
        }

        tenant.setName(name.trim());
        tenant.setConfig(config);

        return tenantRepository.save(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "tenants.delete", description = "Time taken to delete tenant")
    public boolean deleteTenant(UUID id) {
        validateId(id);
        
        if (!tenantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tenant não encontrado com ID: " + id);
        }

        tenantRepository.deleteById(id);
        return true;
    }

    @Override
    public boolean existsTenantById(UUID id) {
        validateId(id);
        return tenantRepository.existsById(id);
    }

    @Override
    public boolean existsTenantByName(String name) {
        validateName(name);
        return tenantRepository.existsByName(name);
    }

    @Override
    @Timed(value = "tenants.count", description = "Time taken to count tenants")
    public long countTenants() {
        return tenantRepository.count();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tenant updateTenantConfig(UUID id, JsonNode config) {
        Tenant tenant = findTenantById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + id));
        
        tenant.setConfig(config);
        
        return tenantRepository.save(tenant);
    }

    @Override
    public JsonNode getTenantConfig(UUID id) {
        Tenant tenant = findTenantById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + id));
        
        return tenant.getConfig();
    }

    @Override
    public List<Tenant> searchTenants(String query) {
        return tenantRepository.search(query);
    }

    @Override
    public long countTenantsCreatedToday() {
        return tenantRepository.countCreatedToday();
    }

    @Override
    public long countTenantsCreatedThisWeek() {
        return tenantRepository.countCreatedThisWeek();
    }

    @Override
    public long countTenantsCreatedThisMonth() {
        return tenantRepository.countCreatedThisMonth();
    }

    @Override
    public long countTenantsInPeriod(LocalDate startDate, LocalDate endDate) {
        return tenantRepository.countInPeriod(startDate.toString(), endDate.toString());
    }

    @Override
    public List<Object> getTenantUsers(UUID tenantId) {
        try {
            List<Object[]> results = tenantRepository.getTenantUsersDetails(tenantId);
            return results.stream()
                    .map(row -> java.util.Map.of(
                        "id", row[0],
                        "name", row[1],
                        "email", row[2],
                        "isActive", row[3],
                        "createdAt", row[4]
                    ))
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter usuários do tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public long countTenantUsers(UUID tenantId) {
        try {
            return tenantRepository.countTenantUsers(tenantId);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar usuários do tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getTenantUsersWithRoles(UUID tenantId) {
        return usageService.getUserTenantsWithRoles(null) // Será implementado com filtro por tenant
                .stream()
                .filter(userTenant -> {
                    // Filtrar por tenantId se necessário
                    return true; // Implementação simplificada
                })
                .toList();
    }

    // Métodos de validação privados
    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("ID do tenant não pode ser nulo");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome do tenant é obrigatório");
        }
        if (name.trim().length() < 2) {
            throw new ValidationException("Nome do tenant deve ter pelo menos 2 caracteres");
        }
    }
}