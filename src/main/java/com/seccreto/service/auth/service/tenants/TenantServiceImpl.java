package com.seccreto.service.auth.service.tenants;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.repository.tenants.TenantRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementação da camada de serviço contendo regras de negócio para tenants.
 * Aplica SRP e DIP com transações declarativas.
 * 
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a multi-tenancy
 * - Configuração JSON flexível
 */
@Service
@Transactional(readOnly = true)
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    public TenantServiceImpl(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "tenants.create", description = "Time taken to create a tenant")
    public Tenant createTenant(String name, JsonNode config) {
        validateName(name);
        
        // Verificar se já existe um tenant com este nome (idempotência)
        Optional<Tenant> existingTenant = tenantRepository.findByNameExact(name.trim());
        if (existingTenant.isPresent()) {
            return existingTenant.get(); // Retorna o tenant existente (idempotência)
        }
        
        Tenant tenant = Tenant.createNew(name.trim(), config);
        Tenant savedTenant = tenantRepository.save(tenant);
        return savedTenant;
    }

    @Override
    public List<Tenant> listAllTenants() {
        return tenantRepository.findAll();
    }

    @Override
    public Optional<Tenant> findTenantById(Long id) {
        validateId(id);
        return tenantRepository.findById(id);
    }

    @Override
    public List<Tenant> findTenantsByName(String name) {
        validateName(name);
        return tenantRepository.findByName(name.trim());
    }

    @Override
    public Optional<Tenant> findTenantByNameExact(String name) {
        validateName(name);
        return tenantRepository.findByNameExact(name.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "tenants.update", description = "Time taken to update a tenant")
    public Tenant updateTenant(Long id, String name, JsonNode config) {
        validateId(id);
        validateName(name);
        
        Tenant existing = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + id));
        
        // Verificar se os dados são diferentes (idempotência)
        if (existing.getName().equals(name.trim()) && 
            ((existing.getConfig() == null && config == null) || 
             (existing.getConfig() != null && existing.getConfig().equals(config)))) {
            return existing; // Retorna o tenant sem alterações (idempotência)
        }
        
        // Verificar se o nome já está em uso por outro tenant
        tenantRepository.findByNameExact(name.trim()).ifPresent(t -> {
            if (!t.getId().equals(id)) {
                throw new ConflictException("Já existe um tenant com este nome");
            }
        });
        
        existing.setName(name.trim());
        existing.setConfig(config);
        Tenant updatedTenant = tenantRepository.update(existing);
        return updatedTenant;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "tenants.delete", description = "Time taken to delete a tenant")
    public boolean deleteTenant(Long id) {
        validateId(id);
        
        // Verificar se o tenant existe antes de tentar deletar (idempotência)
        if (!tenantRepository.existsById(id)) {
            return false; // Tenant já não existe (idempotência)
        }
        
        boolean deleted = tenantRepository.deleteById(id);
        return deleted;
    }

    @Override
    public boolean existsTenantById(Long id) {
        validateId(id);
        return tenantRepository.existsById(id);
    }

    @Override
    public boolean existsTenantByName(String name) {
        validateName(name);
        return tenantRepository.existsByName(name.trim());
    }

    @Override
    public long countTenants() {
        return tenantRepository.count();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "tenants.create", description = "Time taken to create a tenant")
    public Tenant createTenant(String name, String description, String domain) {
        validateName(name);
        
        // Verificar se já existe um tenant com este nome (idempotência)
        Optional<Tenant> existingTenant = tenantRepository.findByNameExact(name.trim());
        if (existingTenant.isPresent()) {
            return existingTenant.get(); // Retorna o tenant existente (idempotência)
        }
        
        Tenant tenant = Tenant.createNew(name.trim(), description, domain);
        Tenant savedTenant = tenantRepository.save(tenant);
        return savedTenant;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "tenants.update", description = "Time taken to update a tenant")
    public Tenant updateTenant(Long id, String name, String description, String domain) {
        validateId(id);
        validateName(name);
        
        Tenant existing = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + id));
        
        // Verificar se o nome já está em uso por outro tenant
        tenantRepository.findByNameExact(name.trim()).ifPresent(t -> {
            if (!t.getId().equals(id)) {
                throw new ConflictException("Já existe um tenant com este nome");
            }
        });
        
        existing.setName(name.trim());
        existing.setDescription(description);
        existing.setDomain(domain);
        Tenant updatedTenant = tenantRepository.update(existing);
        return updatedTenant;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tenant deactivateTenant(Long id) {
        validateId(id);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + id));
        tenant.setActive(false);
        return tenantRepository.update(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tenant activateTenant(Long id) {
        validateId(id);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + id));
        tenant.setActive(true);
        return tenantRepository.update(tenant);
    }

    @Override
    public long countActiveTenants() {
        return tenantRepository.countByActive(true);
    }

    @Override
    public long countInactiveTenants() {
        return tenantRepository.countByActive(false);
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
    public long countTenantsInPeriod(String startDate, String endDate) {
        return tenantRepository.countInPeriod(startDate, endDate);
    }

    @Override
    public List<Tenant> searchTenants(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return tenantRepository.search(query.trim());
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome não pode ser vazio");
        }
        if (name.trim().length() < 2) {
            throw new ValidationException("Nome deve ter pelo menos 2 caracteres");
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
