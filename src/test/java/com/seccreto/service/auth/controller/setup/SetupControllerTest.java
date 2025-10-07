package com.seccreto.service.auth.controller.setup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seccreto.service.auth.api.dto.tenants.TenantRequest;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.service.landlords.LandlordService;
import com.seccreto.service.auth.service.setup.SetupService;
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

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
    void shouldListNetworks() throws Exception {
        when(landlordService.listAllLandlords()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/setup/networks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$" ).isArray())
                .andExpect(jsonPath("$" ).isEmpty());

        verify(landlordService).listAllLandlords();
    }
}
