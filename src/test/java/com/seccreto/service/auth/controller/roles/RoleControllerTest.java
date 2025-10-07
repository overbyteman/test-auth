package com.seccreto.service.auth.controller.roles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.roles.MyRolesResponse;
import com.seccreto.service.auth.api.dto.roles.RoleRequest;
import com.seccreto.service.auth.api.dto.roles.RoleResponse;
import com.seccreto.service.auth.api.dto.roles.RoleUpdateRequest;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.service.auth.AuthService;
import com.seccreto.service.auth.service.roles.RoleService;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

        @Mock
        private RoleService roleService;

        @Mock
        private AuthService authService;

    @InjectMocks
    private RoleController roleController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldListRoles() throws Exception {
        UUID landlordId = UUID.randomUUID();
        Role role = sampleRole(landlordId, "owner", "Owner");
        when(roleService.listRoles(landlordId)).thenReturn(List.of(role));

        mockMvc.perform(get("/api/roles")
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(role.getId().toString()))
                .andExpect(jsonPath("$[0].code").value("owner"))
                .andExpect(jsonPath("$[0].landlordId").value(landlordId.toString()))
                .andExpect(jsonPath("$[0].name").value("Owner"));

        verify(roleService).listRoles(landlordId);
    }

    @Test
    void shouldCreateRole() throws Exception {
        UUID landlordId = UUID.randomUUID();
        RoleRequest request = RoleRequest.builder()
                .code("manager")
                .landlordId(landlordId)
                .name("Manager")
                .description("Gestor geral")
                .build();

        Role createdRole = sampleRole(landlordId, "manager", "Manager");
        when(roleService.createRole(eq(landlordId), eq("manager"), eq("Manager"), eq("Gestor geral")))
                .thenReturn(createdRole);

        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdRole.getId().toString()))
                .andExpect(jsonPath("$.code").value("manager"))
                .andExpect(jsonPath("$.name").value("Manager"));

        verify(roleService).createRole(landlordId, "manager", "Manager", "Gestor geral");
    }

    @Test
    void shouldGetRoleById() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Role role = sampleRole(landlordId, "admin", "Admin");
        role.setId(roleId);
        when(roleService.findRoleById(landlordId, roleId)).thenReturn(Optional.of(role));

        mockMvc.perform(get("/api/roles/{id}", roleId)
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roleId.toString()))
                .andExpect(jsonPath("$.code").value("admin"));

        verify(roleService).findRoleById(landlordId, roleId);
    }

    @Test
    void shouldReturnNotFoundWhenRoleMissing() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(roleService.findRoleById(landlordId, roleId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/roles/{id}", roleId)
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isNotFound());

        verify(roleService).findRoleById(landlordId, roleId);
    }

    @Test
    void shouldUpdateRole() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        RoleUpdateRequest request = RoleUpdateRequest.builder()
                .landlordId(landlordId)
                .name("Supervisor")
                .description("Role atualizado")
                .build();

        Role updatedRole = sampleRole(landlordId, "supervisor", "Supervisor");
        updatedRole.setId(roleId);
        when(roleService.updateRole(eq(landlordId), eq(roleId), eq("Supervisor"), eq("Role atualizado")))
                .thenReturn(updatedRole);

        mockMvc.perform(put("/api/roles/{id}", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roleId.toString()))
                .andExpect(jsonPath("$.name").value("Supervisor"));

        verify(roleService).updateRole(landlordId, roleId, "Supervisor", "Role atualizado");
    }

    @Test
    void shouldDeleteRole() throws Exception {
        UUID landlordId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(roleService.deleteRole(landlordId, roleId)).thenReturn(true);

        mockMvc.perform(delete("/api/roles/{id}", roleId)
                        .param("landlordId", landlordId.toString()))
                .andExpect(status().isNoContent());

        verify(roleService).deleteRole(landlordId, roleId);
    }

    @Test
    void shouldSearchRoles() throws Exception {
        UUID landlordId = UUID.randomUUID();
        RoleResponse roleResponse = RoleResponse.builder()
                .id(UUID.randomUUID())
                .code("coach")
                .name("Coach")
                .description("Treinador")
                .landlordId(landlordId)
                .landlordName("Acme Holdings")
                .build();

        Pagination<RoleResponse> pagination = new Pagination<>(1, 5, 1, List.of(roleResponse));
        when(roleService.searchRoles(eq(landlordId), any(SearchQuery.class))).thenReturn(pagination);

        mockMvc.perform(get("/api/roles/search")
                        .param("landlordId", landlordId.toString())
                        .param("page", "1")
                        .param("perPage", "5")
                        .param("terms", "coach")
                        .param("sort", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.items[0].code").value("coach"))
                .andExpect(jsonPath("$.itemsCount").value(1));

        verify(roleService).searchRoles(eq(landlordId), any(SearchQuery.class));
    }

        @Test
        void shouldReturnMyRoles() throws Exception {
                UUID landlordId = UUID.randomUUID();
                UUID tenantId = UUID.randomUUID();
                MyRolesResponse response = MyRolesResponse.builder()
                                .landlordId(landlordId)
                                .landlordName("Academia Central")
                                .tenantId(tenantId)
                                .tenantName("Unidade Copacabana")
                                .roles(List.of("ADMIN", "MANAGER"))
                                .permissions(List.of("manage:users", "read:dashboard"))
                                .build();

                when(authService.getCurrentUserRoles("Bearer token", tenantId)).thenReturn(List.of(response));

                mockMvc.perform(get("/api/roles/me")
                                                .header("Authorization", "Bearer token")
                                                .param("tenantId", tenantId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].landlordId").value(landlordId.toString()))
                                .andExpect(jsonPath("$[0].tenantId").value(tenantId.toString()))
                                .andExpect(jsonPath("$[0].roles[0]").value("ADMIN"))
                                .andExpect(jsonPath("$[0].permissions[1]").value("read:dashboard"));

                verify(authService).getCurrentUserRoles("Bearer token", tenantId);
        }

    private Role sampleRole(UUID landlordId, String code, String name) {
        Landlord landlord = Landlord.builder()
                .id(landlordId)
                .name("Landlord " + landlordId.toString().substring(0, 6))
                .config(objectMapper.createObjectNode())
                .build();

        Role role = Role.builder()
                .id(UUID.randomUUID())
                .code(code)
                .name(name)
                .description("Descrição " + name)
                .landlord(landlord)
                .build();

        landlord.addRole(role);
        return role;
    }
}
