package com.seccreto.service.auth.controller.setup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seccreto.service.auth.api.dto.landlords.LandlordResponse;
import com.seccreto.service.auth.api.dto.tenants.TenantRequest;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.controller.setup.SetupController.NetworkSetupRequest;
import com.seccreto.service.auth.controller.setup.SetupController.CompleteNetworkRequest;
import com.seccreto.service.auth.service.landlords.LandlordService;
import com.seccreto.service.auth.service.setup.SetupService;
import com.seccreto.service.auth.service.tenants.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SetupControllerTest {

    @Mock
    private SetupService setupService;

    @Mock
    private LandlordService landlordService;

    @Mock
    private TenantService tenantService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SetupController setupController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(setupController).build();
    }

    @Test
    void shouldCreateNetworkWithRoles() throws Exception {
        NetworkSetupRequest request = new NetworkSetupRequest();
        request.setName("Acme Network");
        request.setDescription("Rede de academias");
        ObjectNode config = objectMapper.createObjectNode().put("currency", "BRL");
        request.setConfig(config);

        LandlordResponse response = LandlordResponse.builder()
                .id(UUID.randomUUID())
                .name("Acme Network")
                .config(config)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .tenantsCount(0)
                .rolesCount(0)
                .build();

        when(setupService.createNetworkWithRoles(anyString(), anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/setup/network")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Network"));

        verify(setupService).createNetworkWithRoles("Acme Network", "Rede de academias", config);
    }

    @Test
    void shouldAddTenantToNetwork() throws Exception {
        UUID landlordId = UUID.randomUUID();
        TenantRequest request = TenantRequest.builder()
                .name("Academia Central")
                .landlordId(landlordId)
                .build();

        TenantResponse response = TenantResponse.builder()
                .id(UUID.randomUUID())
                .name("Academia Central")
                .landlordId(landlordId)
                .landlordName("Acme Network")
                .active(true)
                .build();

        when(setupService.addTenantToNetwork(any(UUID.class), any(TenantRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/setup/network/{landlordId}/tenant", landlordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Academia Central"))
                .andExpect(jsonPath("$.landlordId").value(landlordId.toString()));

                // Use argument matchers since the controller builds a new TenantRequest instance during mapping
                verify(setupService).addTenantToNetwork(eq(landlordId), any(TenantRequest.class));
    }

    @Test
    void shouldSetupDefaultRolesForLandlord() throws Exception {
        UUID landlordId = UUID.randomUUID();
        when(setupService.setupDefaultRolesForLandlord(landlordId)).thenReturn(4);

        mockMvc.perform(post("/api/setup/network/{landlordId}/roles", landlordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Roles padrões configurados com sucesso"))
                .andExpect(jsonPath("$.rolesCreatedCount").value(4));

        verify(setupService).setupDefaultRolesForLandlord(landlordId);
    }

    @Test
    void shouldReturnNetworkStatus() throws Exception {
        UUID landlordId = UUID.randomUUID();
        SetupService.NetworkStatus statusResponse = new SetupService.NetworkStatus();
        statusResponse.setHasRoles(true);
        statusResponse.setHasPolicies(true);
        statusResponse.setPermissionsCount(10);
        statusResponse.setRolesCount(4);
        statusResponse.setTenantsCount(2);

        when(setupService.getNetworkStatus(landlordId)).thenReturn(statusResponse);

        mockMvc.perform(get("/api/setup/network/{landlordId}/status", landlordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.networkStatus.rolesCount").value(4))
                .andExpect(jsonPath("$.networkStatus.hasRoles").value(true));

        verify(setupService).getNetworkStatus(landlordId);
    }

    @Test
    void shouldListNetworks() throws Exception {
        when(landlordService.listAllLandlords()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/setup/networks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$" ).isArray())
                .andExpect(jsonPath("$" ).isEmpty());

        verify(landlordService).listAllLandlords();
    }

    @Test
    void shouldCreateCompleteNetwork() throws Exception {
        CompleteNetworkRequest request = new CompleteNetworkRequest();
        request.setNetworkName("Acme Network");
        request.setNetworkDescription("Rede completa");
        request.setFirstTenantName("Academia Central");

        SetupService.CompleteNetworkResponse response = new SetupService.CompleteNetworkResponse();
        LandlordResponse landlordResponse = LandlordResponse.builder()
                .id(UUID.randomUUID())
                .name("Acme Network")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        TenantResponse tenantResponse = TenantResponse.builder()
                .id(UUID.randomUUID())
                .name("Academia Central")
                .active(true)
                .build();
        response.setLandlord(landlordResponse);
        response.setFirstTenant(tenantResponse);
        response.setRolesCreated(4);
        response.setPoliciesCreated(3);
        response.setPermissionsCreated(10);
        response.setTotalExecutionTime(150L);

        when(setupService.createCompleteNetwork(any(SetupService.CompleteNetworkRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/setup/network/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.landlord.name").value("Acme Network"))
                .andExpect(jsonPath("$.firstTenant.name").value("Academia Central"))
                .andExpect(jsonPath("$.rolesCreated").value(4));

        verify(setupService).createCompleteNetwork(any(SetupService.CompleteNetworkRequest.class));
    }
}
