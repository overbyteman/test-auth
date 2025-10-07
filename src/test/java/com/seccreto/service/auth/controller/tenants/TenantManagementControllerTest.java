package com.seccreto.service.auth.controller.tenants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.tenants.TenantRequest;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.service.tenants.TenantService;
import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenantManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private UserService userService;

        @MockBean
        private JwtService jwtService;

    @Test
    void shouldListAllTenants() throws Exception {
        Tenant tenant = sampleTenant(true);
        when(tenantService.listAllTenants()).thenReturn(List.of(tenant));

        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(tenant.getId().toString()))
                .andExpect(jsonPath("$[0].name").value(tenant.getName()))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].landlordName").value(tenant.getLandlord().getName()));

        verify(tenantService).listAllTenants();
    }

    @Test
    void shouldCreateTenant() throws Exception {
        Tenant tenant = sampleTenant(true);
        TenantRequest request = TenantRequest.builder()
                .name(tenant.getName())
                .config((ObjectNode) tenant.getConfig())
                .landlordId(tenant.getLandlord().getId())
                .build();

        when(tenantService.createTenant(eq(tenant.getName()), any(), eq(tenant.getLandlord().getId())))
                .thenReturn(tenant);

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.name").value(tenant.getName()));

        ArgumentCaptor<ObjectNode> configCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(tenantService).createTenant(eq(tenant.getName()), configCaptor.capture(), eq(tenant.getLandlord().getId()));
        assertThat(configCaptor.getValue().get("timezone").asText()).isEqualTo("UTC");
    }

    @Test
    void shouldActivateTenant() throws Exception {
        Tenant tenant = sampleTenant(false);
        tenant.activate();
        when(tenantService.activateTenant(tenant.getId())).thenReturn(tenant);

        mockMvc.perform(post("/api/tenants/{id}/activate", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.active").value(true));

        verify(tenantService).activateTenant(tenant.getId());
    }

        @Test
        void shouldReturnTenantDetails() throws Exception {
                Tenant tenant = sampleTenant(true);
                when(tenantService.findTenantById(tenant.getId())).thenReturn(java.util.Optional.of(tenant));

                mockMvc.perform(get("/api/tenants/{id}", tenant.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(tenant.getId().toString()))
                                .andExpect(jsonPath("$.landlordName").value(tenant.getLandlord().getName()));

                verify(tenantService).findTenantById(tenant.getId());
        }

        @Test
        void shouldReturnNotFoundWhenTenantMissing() throws Exception {
                UUID tenantId = UUID.randomUUID();
                when(tenantService.findTenantById(tenantId)).thenReturn(java.util.Optional.empty());

                mockMvc.perform(get("/api/tenants/{id}", tenantId))
                                .andExpect(status().isNotFound());

                verify(tenantService).findTenantById(tenantId);
        }

    @Test
    void shouldDeactivateTenant() throws Exception {
        Tenant tenant = sampleTenant(true);

        mockMvc.perform(delete("/api/tenants/{id}", tenant.getId()))
                .andExpect(status().isNoContent());

        verify(tenantService).deactivateTenant(tenant.getId());
    }

    @Test
    void shouldSearchTenants() throws Exception {
        Tenant tenant = sampleTenant(true);
        TenantResponse tenantResponse = TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .config(tenant.getConfig())
                .landlordId(tenant.getLandlord().getId())
                .landlordName(tenant.getLandlord().getName())
                .active(tenant.isActive())
                .deactivatedAt(tenant.getDeactivatedAt())
                .build();

        Pagination<TenantResponse> pagination = new Pagination<>(1, 10, 1, List.of(tenantResponse));
        when(tenantService.searchTenants(any(SearchQuery.class))).thenReturn(pagination);

        mockMvc.perform(get("/api/tenants/search")
                        .param("page", "1")
                        .param("perPage", "10")
                        .param("terms", "Academia")
                        .param("sort", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.items[0].id").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.itemsCount").value(1));

        verify(tenantService).searchTenants(any(SearchQuery.class));
    }

    @Test
    void shouldUpdateTenant() throws Exception {
        Tenant tenant = sampleTenant(true);
        TenantRequest request = TenantRequest.builder()
                .name("Academia Atualizada")
                .config((ObjectNode) tenant.getConfig())
                .landlordId(tenant.getLandlord().getId())
                .build();

        tenant.setName("Academia Atualizada");
        when(tenantService.updateTenant(eq(tenant.getId()), eq("Academia Atualizada"), any(), eq(tenant.getLandlord().getId())))
                .thenReturn(tenant);

        mockMvc.perform(put("/api/tenants/{id}", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Academia Atualizada"));

        verify(tenantService).updateTenant(eq(tenant.getId()), eq("Academia Atualizada"), any(), eq(tenant.getLandlord().getId()));
    }

    private Tenant sampleTenant(boolean active) {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("timezone", "UTC");
        Landlord landlord = Landlord.createNew("Acme Holdings", objectMapper.createObjectNode());
        landlord.setId(UUID.randomUUID());
        landlord.setCreatedAt(LocalDateTime.now());
        landlord.setUpdatedAt(LocalDateTime.now());

        Tenant tenant = Tenant.createNew("Academia Central", config, landlord);
        tenant.setId(UUID.randomUUID());
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());
        if (!active) {
            tenant.deactivate();
        }
        return tenant;
    }
}
