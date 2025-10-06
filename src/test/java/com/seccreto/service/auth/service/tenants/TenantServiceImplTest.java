package com.seccreto.service.auth.service.tenants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.repository.landlords.LandlordRepository;
import com.seccreto.service.auth.repository.tenants.TenantRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.usage.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private LandlordRepository landlordRepository;

    @Mock
    private UsageService usageService;

    @InjectMocks
    private TenantServiceImpl tenantService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Landlord landlord;
    private UUID landlordId;

    @BeforeEach
    void setUp() {
        landlordId = UUID.randomUUID();
        landlord = Landlord.createNew("Acme Holdings", createConfig("tier", "enterprise"));
        landlord.setId(landlordId);
        landlord.setCreatedAt(LocalDateTime.now());
        landlord.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createTenantShouldPersistNewTenant() {
        String name = "Academia Central ";
        JsonNode config = createConfig("timezone", "UTC");
        Tenant persisted = tenantWithId(UUID.randomUUID(), name.trim(), config, landlord);

        when(landlordRepository.findById(landlordId)).thenReturn(Optional.of(landlord));
        when(tenantRepository.findByName(name.trim())).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(persisted);

        Tenant result = tenantService.createTenant(name, config, landlordId);

        assertThat(result).isEqualTo(persisted);

        verify(landlordRepository).findById(landlordId);
        verify(tenantRepository).findByName(name.trim());

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        Tenant toSave = tenantCaptor.getValue();
        assertThat(toSave.getName()).isEqualTo(name.trim());
        assertThat(toSave.getConfig()).isEqualTo(config);
        assertThat(toSave.getLandlord()).isEqualTo(landlord);
    }

    @Test
    void createTenantShouldReturnExistingWhenAlreadyAssociated() {
        String name = "Academia Central";
        JsonNode config = createConfig("timezone", "UTC");
        Tenant existing = tenantWithId(UUID.randomUUID(), name, config, landlord);

        when(landlordRepository.findById(landlordId)).thenReturn(Optional.of(landlord));
        when(tenantRepository.findByName(name)).thenReturn(Optional.of(existing));

        Tenant result = tenantService.createTenant(name, config, landlordId);

        assertThat(result).isSameAs(existing);
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void createTenantShouldAttachLandlordWhenMissing() {
        String name = "Academia Central";
        JsonNode config = createConfig("timezone", "UTC");
        Tenant orphanTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name(name)
                .config(config)
                .active(true)
                .build();

        when(landlordRepository.findById(landlordId)).thenReturn(Optional.of(landlord));
        when(tenantRepository.findByName(name)).thenReturn(Optional.of(orphanTenant));
        when(tenantRepository.save(orphanTenant)).thenReturn(orphanTenant);

        Tenant result = tenantService.createTenant(name, config, landlordId);

        assertThat(result.getLandlord()).isEqualTo(landlord);
        verify(tenantRepository).save(orphanTenant);
    }

    @Test
    void createTenantShouldThrowConflictWhenBelongsToAnotherLandlord() {
        String name = "Academia Central";
        JsonNode config = createConfig("timezone", "UTC");
        Landlord otherLandlord = Landlord.createNew("Another", createConfig("tier", "basic"));
        otherLandlord.setId(UUID.randomUUID());
        Tenant existing = tenantWithId(UUID.randomUUID(), name, config, otherLandlord);

        when(landlordRepository.findById(landlordId)).thenReturn(Optional.of(landlord));
        when(tenantRepository.findByName(name)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tenantService.createTenant(name, config, landlordId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Tenant já existe associado a outro landlord");

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void createTenantShouldThrowWhenLandlordMissing() {
        String name = "Academia Central";
        JsonNode config = createConfig("timezone", "UTC");

        when(landlordRepository.findById(landlordId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.createTenant(name, config, landlordId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Landlord não encontrado com ID: " + landlordId);

        verify(tenantRepository, never()).findByName(name);
    }

    @Test
    void createTenantShouldValidateName() {
        JsonNode config = createConfig("timezone", "UTC");

        assertThatThrownBy(() -> tenantService.createTenant(" ", config, landlordId))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Nome do tenant é obrigatório");

        assertThatThrownBy(() -> tenantService.createTenant("A", config, landlordId))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Nome do tenant deve ter pelo menos 2 caracteres");
    }

    @Test
    void createTenantShouldValidateLandlordId() {
        JsonNode config = createConfig("timezone", "UTC");

        assertThatThrownBy(() -> tenantService.createTenant("Academia Central", config, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Identificador não pode ser nulo");
    }

    @Test
    void updateTenantShouldApplyChanges() {
        UUID tenantId = UUID.randomUUID();
        JsonNode initialConfig = createConfig("timezone", "UTC");
        Tenant tenant = tenantWithId(tenantId, "Academia Central", initialConfig, landlord);

        JsonNode newConfig = createConfig("timezone", "America/Sao_Paulo");
        String newName = "Academia Central Atualizada";
        Landlord newLandlord = Landlord.createNew("Nova Matriz", createConfig("tier", "enterprise"));
        newLandlord.setId(UUID.randomUUID());
        newLandlord.setCreatedAt(LocalDateTime.now());
        newLandlord.setUpdatedAt(LocalDateTime.now());

        when(landlordRepository.findById(newLandlord.getId())).thenReturn(Optional.of(newLandlord));
        when(tenantRepository.findByIdWithLandlord(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.findByName(newName.trim())).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant result = tenantService.updateTenant(tenantId, newName, newConfig, newLandlord.getId());

        assertThat(result.getName()).isEqualTo(newName.trim());
        assertThat(result.getConfig()).isEqualTo(newConfig);
        assertThat(result.getLandlord()).isEqualTo(newLandlord);

        verify(tenantRepository).save(tenant);
    }

    @Test
    void updateTenantShouldThrowConflictWhenNameInUseByOtherTenant() {
        UUID tenantId = UUID.randomUUID();
        JsonNode config = createConfig("timezone", "UTC");
        Tenant tenant = tenantWithId(tenantId, "Academia Central", config, landlord);
        Tenant another = tenantWithId(UUID.randomUUID(), "Academia Central Atualizada", config, landlord);

        when(landlordRepository.findById(landlordId)).thenReturn(Optional.of(landlord));
        when(tenantRepository.findByIdWithLandlord(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.findByName("Academia Central Atualizada")).thenReturn(Optional.of(another));

        assertThatThrownBy(() -> tenantService.updateTenant(tenantId, "Academia Central Atualizada", config, landlordId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Nome já está em uso por outro tenant");
    }

    @Test
    void updateTenantShouldThrowWhenLandlordMissing() {
        UUID tenantId = UUID.randomUUID();
        JsonNode config = createConfig("timezone", "UTC");

        when(landlordRepository.findById(landlordId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.updateTenant(tenantId, "Academia", config, landlordId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Landlord não encontrado com ID: " + landlordId);
    }

    @Test
    void updateTenantShouldThrowWhenTenantMissing() {
        UUID tenantId = UUID.randomUUID();
        JsonNode config = createConfig("timezone", "UTC");

        when(landlordRepository.findById(landlordId)).thenReturn(Optional.of(landlord));
        when(tenantRepository.findByIdWithLandlord(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.updateTenant(tenantId, "Academia", config, landlordId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tenant não encontrado com ID: " + tenantId);
    }

    @Test
    void deleteTenantShouldRemoveEntity() {
        UUID tenantId = UUID.randomUUID();

        when(tenantRepository.existsById(tenantId)).thenReturn(true);

        boolean result = tenantService.deleteTenant(tenantId);

        assertThat(result).isTrue();
        verify(tenantRepository).deleteById(tenantId);
    }

    @Test
    void deleteTenantShouldThrowWhenMissing() {
        UUID tenantId = UUID.randomUUID();

        when(tenantRepository.existsById(tenantId)).thenReturn(false);

        assertThatThrownBy(() -> tenantService.deleteTenant(tenantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tenant não encontrado com ID: " + tenantId);
    }

    @Test
    void deactivateTenantShouldChangeStatus() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenantWithId(tenantId, "Academia", createConfig("timezone", "UTC"), landlord);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        Tenant result = tenantService.deactivateTenant(tenantId);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getDeactivatedAt()).isNotNull();
    }

    @Test
    void activateTenantShouldChangeStatus() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenantWithId(tenantId, "Academia", createConfig("timezone", "UTC"), landlord);
        tenant.deactivate();

        when(tenantRepository.findByIdWithLandlord(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        Tenant result = tenantService.activateTenant(tenantId);

        assertThat(result.isActive()).isTrue();
        assertThat(result.getDeactivatedAt()).isNull();
    }

    @Test
    void listAllTenantsShouldReturnRepositoryResult() {
        List<Tenant> tenants = List.of(
                tenantWithId(UUID.randomUUID(), "Academia Central", createConfig("timezone", "UTC"), landlord),
                tenantWithId(UUID.randomUUID(), "Academia Norte", createConfig("timezone", "UTC"), landlord)
        );

        when(tenantRepository.findAllWithLandlord()).thenReturn(tenants);

        List<Tenant> result = tenantService.listAllTenants();

        assertThat(result).isEqualTo(tenants);
    }

    @Test
    void findTenantByIdShouldDelegateToRepository() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = tenantWithId(tenantId, "Academia", createConfig("timezone", "UTC"), landlord);

        when(tenantRepository.findByIdWithLandlord(tenantId)).thenReturn(Optional.of(tenant));

        Optional<Tenant> result = tenantService.findTenantById(tenantId);

        assertThat(result).contains(tenant);
    }

    @Test
    void findTenantsByNameShouldReturnList() {
        String name = "Academia";
        List<Tenant> tenants = List.of(tenantWithId(UUID.randomUUID(), name, createConfig("timezone", "UTC"), landlord));

        when(tenantRepository.findByName(name)).thenReturn(Optional.of(tenants.get(0)));

        List<Tenant> result = tenantService.findTenantsByName(name);

        assertThat(result).containsExactlyElementsOf(tenants);
    }

    @Test
    void findTenantsByLandlordShouldValidateId() {
        assertThatThrownBy(() -> tenantService.findTenantsByLandlordId(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Identificador não pode ser nulo");
    }

    @Test
    void countTenantsByLandlordShouldDelegate() {
        when(tenantRepository.countByLandlordId(landlordId)).thenReturn(5L);

        long result = tenantService.countTenantsByLandlordId(landlordId);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void existsTenantByNameShouldDelegate() {
        when(tenantRepository.existsByName("Academia")).thenReturn(true);

        assertThat(tenantService.existsTenantByName("Academia")).isTrue();
    }

    @Test
    void countTenantsCreatedTodayShouldDelegate() {
        when(tenantRepository.countCreatedToday()).thenReturn(3L);

        assertThat(tenantService.countTenantsCreatedToday()).isEqualTo(3L);
    }

    @Test
    void countTenantsCreatedThisWeekShouldDelegate() {
        when(tenantRepository.countCreatedThisWeek()).thenReturn(7L);

        assertThat(tenantService.countTenantsCreatedThisWeek()).isEqualTo(7L);
    }

    @Test
    void countTenantsCreatedThisMonthShouldDelegate() {
        when(tenantRepository.countCreatedThisMonth()).thenReturn(12L);

        assertThat(tenantService.countTenantsCreatedThisMonth()).isEqualTo(12L);
    }

    @Test
    void countTenantsInPeriodShouldDelegate() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        when(tenantRepository.countInPeriod(start.toString(), end.toString())).thenReturn(20L);

        assertThat(tenantService.countTenantsInPeriod(start, end)).isEqualTo(20L);
    }

    private JsonNode createConfig(String key, String value) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(key, value);
        return node;
    }

    private Tenant tenantWithId(UUID id, String name, JsonNode config, Landlord owner) {
        Tenant tenant = Tenant.createNew(name, config, owner);
        tenant.setId(id);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());
        return tenant;
    }
}
