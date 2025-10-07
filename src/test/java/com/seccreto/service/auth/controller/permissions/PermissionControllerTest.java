package com.seccreto.service.auth.controller.permissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.permissions.PermissionPolicyPresetResponse;
import com.seccreto.service.auth.api.dto.permissions.PermissionRequest;
import com.seccreto.service.auth.api.dto.permissions.PermissionResponse;
import com.seccreto.service.auth.api.dto.permissions.PermissionUpdateRequest;
import com.seccreto.service.auth.api.exception.GlobalExceptionHandler;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;
import com.seccreto.service.auth.service.permissions.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController permissionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(permissionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldCreatePermission() throws Exception {
        UUID landlordId = UUID.randomUUID();
        PermissionRequest request = PermissionRequest.builder()
                .landlordId(landlordId)
                .action("create")
                .resource("members")
                .name("Criar membros")
                .description("Permite criar membros")
                .build();

        Permission permission = samplePermission(landlordId);
        when(permissionService.createPermission(eq(landlordId), eq("create"), eq("members")))
                .thenReturn(permission);

        mockMvc.perform(post("/api/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(permission.getId().toString()))
                .andExpect(jsonPath("$.action").value("create"))
                .andExpect(jsonPath("$.resource").value("members"));

        verify(permissionService).createPermission(landlordId, "create", "members");
    }

    @Test
    void shouldListPermissions() throws Exception {
        UUID landlordId = UUID.randomUUID();
        Permission permission = samplePermission(landlordId);
        when(permissionService.listPermissions(landlordId)).thenReturn(List.of(permission));

        mockMvc.perform(get("/api/permissions")
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(permission.getId().toString()))
                .andExpect(jsonPath("$[0].permissionString").value("create:members"));

        verify(permissionService).listPermissions(landlordId);
    }

    @Test
    void shouldSearchPermissions() throws Exception {
        UUID landlordId = UUID.randomUUID();
        PermissionResponse response = PermissionResponse.builder()
                .id(UUID.randomUUID())
                .action("read")
                .resource("members")
                .landlordId(landlordId)
                .permissionString("read:members")
                .build();

        Pagination<PermissionResponse> pagination = new Pagination<>(1, 10, 1, List.of(response));
        when(permissionService.searchPermissions(eq(landlordId), any(SearchQuery.class))).thenReturn(pagination);

        mockMvc.perform(get("/api/permissions/search")
                        .param("landlordId", landlordId.toString())
                        .param("page", "1")
                        .param("perPage", "10")
                        .param("terms", "members")
                        .param("sort", "action")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.items[0].permissionString").value("read:members"))
                .andExpect(jsonPath("$.itemsCount").value(1));

        verify(permissionService).searchPermissions(eq(landlordId), any(SearchQuery.class));
    }

    @Test
    void shouldGetPermissionById() throws Exception {
        UUID landlordId = UUID.randomUUID();
        Permission permission = samplePermission(landlordId);
        when(permissionService.findPermissionById(landlordId, permission.getId())).thenReturn(Optional.of(permission));

        mockMvc.perform(get("/api/permissions/{id}", permission.getId())
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(permission.getId().toString()))
                .andExpect(jsonPath("$.policyName").value("Admin Full Access"));

        verify(permissionService).findPermissionById(landlordId, permission.getId());
    }

    @Test
    void shouldReturnNotFoundWhenPermissionMissing() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        when(permissionService.findPermissionById(landlordId, permissionId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/permissions/{id}", permissionId)
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isNotFound());

        verify(permissionService).findPermissionById(landlordId, permissionId);
    }

    @Test
    void shouldUpdatePermission() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        Permission current = samplePermission(landlordId);
        current.setId(permissionId);

        Permission updated = samplePermission(landlordId);
        updated.setId(permissionId);
        updated.setAction("update");

        PermissionUpdateRequest request = PermissionUpdateRequest.builder()
                .action("update")
                .build();

        when(permissionService.findPermissionById(landlordId, permissionId)).thenReturn(Optional.of(current));
        when(permissionService.updatePermission(landlordId, permissionId, "update", "members"))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/permissions/{id}", permissionId)
                        .param("landlordId", landlordId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("update"));

        verify(permissionService).updatePermission(landlordId, permissionId, "update", "members");
    }

    @Test
    void shouldReturnBadRequestWhenUpdateHasNoChanges() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        PermissionUpdateRequest request = PermissionUpdateRequest.builder().build();

        mockMvc.perform(patch("/api/permissions/{id}", permissionId)
                        .param("landlordId", landlordId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeletePermission() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();

        mockMvc.perform(delete("/api/permissions/{id}", permissionId)
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isNoContent());

        verify(permissionService).deletePermission(landlordId, permissionId);
    }

    @Test
    void shouldListPolicyPresets() throws Exception {
        PermissionPolicyPresetResponse preset = PermissionPolicyPresetResponse.builder()
                .code("admin-full-access")
                .name("Admin Full Access")
                .effect("ALLOW")
                .recommendedActions(List.of("create", "read"))
                .recommendedResources(List.of("users"))
                .recommendedIpRanges(List.of("10.0.0.0/16"))
                .recommendedSchedules(List.of(PermissionPolicyPresetResponse.ScheduleWindow.builder()
                        .timezone("America/Sao_Paulo")
                        .days(List.of("MONDAY"))
                        .start("08:00")
                        .end("18:00")
                        .build()))
                .build();

        when(permissionService.listPolicyPresets()).thenReturn(List.of(preset));

        mockMvc.perform(get("/api/permissions/policy-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("admin-full-access"))
                .andExpect(jsonPath("$[0].recommendedIpRanges[0]").value("10.0.0.0/16"))
                .andExpect(jsonPath("$[0].recommendedSchedules[0].timezone").value("America/Sao_Paulo"));

        verify(permissionService).listPolicyPresets();
    }

    private Permission samplePermission(UUID landlordId) {
        Landlord landlord = Landlord.builder()
                .id(landlordId)
                .name("Landlord " + landlordId.toString().substring(0, 6))
                .config(objectMapper.createObjectNode())
                .build();

        Policy policy = Policy.builder()
                .id(UUID.randomUUID())
                .code("admin-full-access")
                .name("Admin Full Access")
                .effect(PolicyEffect.ALLOW)
                .build();

        return Permission.builder()
                .id(UUID.randomUUID())
                .action("create")
                .resource("members")
                .landlord(landlord)
                .policy(policy)
                .build();
    }
}
