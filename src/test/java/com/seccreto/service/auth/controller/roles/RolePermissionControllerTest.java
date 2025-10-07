package com.seccreto.service.auth.controller.roles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seccreto.service.auth.api.dto.roles_permissions.RolePermissionAttachRequest;
import com.seccreto.service.auth.api.dto.roles_permissions.RolePermissionPolicyUpdateRequest;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.service.rolespermissions.RolePermissionService;
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
class RolePermissionControllerTest {

    @Mock
    private RolePermissionService rolePermissionService;

    @InjectMocks
    private RolePermissionController rolePermissionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rolePermissionController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldAttachPermissionToRole() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        RolePermissionAttachRequest request = RolePermissionAttachRequest.builder()
                .permissionId(permissionId)
                .policyId(null)
                .inheritPermissionPolicy(true)
                .build();

        RolesPermissions association = sampleAssociation(roleId, permissionId);
        when(rolePermissionService.attachPermission(eq(landlordId), eq(roleId), eq(permissionId), any(), eq(true)))
                .thenReturn(association);

        mockMvc.perform(post("/api/roles/{roleId}/permissions", roleId)
                        .param("landlordId", landlordId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permissionString").value("read:members"))
                .andExpect(jsonPath("$.policyCode").value("reception-access"));

        verify(rolePermissionService).attachPermission(landlordId, roleId, permissionId, null, true);
    }

    @Test
    void shouldUpdateAssociationPolicy() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();

        RolePermissionPolicyUpdateRequest request = RolePermissionPolicyUpdateRequest.builder()
                .policyId(policyId)
                .inheritPermissionPolicy(false)
                .build();

        RolesPermissions association = sampleAssociation(roleId, permissionId);
        when(rolePermissionService.updatePermissionPolicy(eq(landlordId), eq(roleId), eq(permissionId), eq(policyId), eq(false)))
                .thenReturn(association);

        mockMvc.perform(patch("/api/roles/{roleId}/permissions/{permissionId}/policy", roleId, permissionId)
                        .param("landlordId", landlordId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(roleId.toString()));

        verify(rolePermissionService).updatePermissionPolicy(landlordId, roleId, permissionId, policyId, false);
    }

    @Test
    void shouldDetachPermissionFromRole() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();

        when(rolePermissionService.detachPermission(landlordId, roleId, permissionId)).thenReturn(true);

        mockMvc.perform(delete("/api/roles/{roleId}/permissions/{permissionId}", roleId, permissionId)
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isNoContent());

        verify(rolePermissionService).detachPermission(landlordId, roleId, permissionId);
    }

    @Test
    void shouldReturnNotFoundWhenDetachingUnknownAssociation() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();

        when(rolePermissionService.detachPermission(landlordId, roleId, permissionId)).thenReturn(false);

        mockMvc.perform(delete("/api/roles/{roleId}/permissions/{permissionId}", roleId, permissionId)
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isNotFound());

        verify(rolePermissionService).detachPermission(landlordId, roleId, permissionId);
    }

    @Test
    void shouldListRolePermissions() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(rolePermissionService.listRolePermissions(eq(landlordId), eq(roleId)))
                .thenReturn(List.of(sampleAssociation(roleId, UUID.randomUUID())));

        mockMvc.perform(get("/api/roles/{roleId}/permissions", roleId)
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyEffect").value("ALLOW"));

        verify(rolePermissionService).listRolePermissions(landlordId, roleId);
    }

    private RolesPermissions sampleAssociation(UUID roleId, UUID permissionId) {
        Landlord landlord = Landlord.builder()
                .id(UUID.randomUUID())
                .name("Landlord")
                .build();

        Role role = Role.builder()
                .id(roleId)
                .code("role-code")
                .name("Role Name")
                .landlord(landlord)
                .build();

        Permission permission = Permission.builder()
                .id(permissionId)
                .action("read")
                .resource("members")
                .landlord(landlord)
                .build();

        Policy policy = Policy.builder()
                .id(UUID.randomUUID())
                .code("reception-access")
                .name("Reception Access")
                .effect(PolicyEffect.ALLOW)
                .tenant(null)
                .build();

        return RolesPermissions.of(role, permission, policy);
    }
}
