package com.seccreto.service.auth.controller.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.users.UserRequest;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.api.dto.users.UserRoleAssignmentRequest;
import com.seccreto.service.auth.api.mapper.users.UserMapper;
import com.seccreto.service.auth.model.sessions.Session;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.service.sessions.SessionService;
import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.users.assignments.UserAssignmentService;
import com.seccreto.service.auth.service.users_tenants_permissions.UsersTenantsPermissionsService;
import com.seccreto.service.auth.service.users_tenants_roles.UsersTenantsRolesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UsersTenantsRolesService usersTenantsRolesService;

        @Mock
        private UsersTenantsPermissionsService usersTenantsPermissionsService;

    @Mock
    private SessionService sessionService;

        @Mock
        private UserAssignmentService userAssignmentService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldListAllUsers() {
        LocalDateTime fixedTime = LocalDateTime.of(2024, Month.JANUARY, 10, 12, 0);
        User user1 = user("John Doe", "john@example.com", fixedTime);
        User user2 = user("Jane Smith", "jane@example.com", fixedTime.plusHours(1));

        when(userService.listAllUsers()).thenReturn(List.of(user1, user2));

        ResponseEntity<List<UserResponse>> result = userController.getAllUsers();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(2);
        assertThat(result.getBody()).extracting(UserResponse::getName)
                .containsExactly("John Doe", "Jane Smith");

        verify(userService).listAllUsers();
    }

    @Test
    void shouldGetUserById() {
        UUID userId = UUID.randomUUID();
        User user = user("John Doe", "john@example.com", LocalDateTime.of(2024, Month.JANUARY, 5, 9, 30));
        user.setId(userId);

        when(userService.findUserById(userId)).thenReturn(Optional.of(user));

        ResponseEntity<UserResponse> result = userController.getUserById(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(userId);
        assertThat(result.getBody().getName()).isEqualTo("John Doe");
        assertThat(result.getBody().getEmail()).isEqualTo("john@example.com");

        verify(userService).findUserById(userId);
    }

    @Test
    void shouldReturnNotFoundForUnknownUser() {
        UUID userId = UUID.randomUUID();
        when(userService.findUserById(userId)).thenReturn(Optional.empty());

        ResponseEntity<UserResponse> result = userController.getUserById(userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isNull();

        verify(userService).findUserById(userId);
    }

    @Test
    void shouldCreateUser() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2024, Month.JANUARY, 12, 14, 45);
        UserRequest request = UserRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("Str0ng!Pwd9X")
                .build();

        User createdUser = user("John Doe", "john@example.com", createdAt);
        createdUser.setId(userId);

        when(userService.createUser(eq("John Doe"), eq("john@example.com"), eq("Str0ng!Pwd9X")))
                .thenReturn(createdUser);
        when(userService.findByEmail("john@example.com")).thenReturn(Optional.of(createdUser));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService).createUser("John Doe", "john@example.com", "Str0ng!Pwd9X");
        verify(userService).findByEmail("john@example.com");
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserRequest request = UserRequest.builder()
                .name("John Updated")
                .email("john.updated@example.com")
                .password("Str0ng!Pwd9X")
                .build();

        User updatedUser = user("John Updated", "john.updated@example.com",
                LocalDateTime.of(2024, Month.JANUARY, 20, 10, 15));
        updatedUser.setId(userId);

        when(userService.updateUser(userId, "John Updated", "john.updated@example.com"))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.email").value("john.updated@example.com"));

        verify(userService).updateUser(userId, "John Updated", "john.updated@example.com");
    }

    @Test
    void shouldActivateUser() {
        UUID userId = UUID.randomUUID();
        when(userService.activateUser(userId)).thenReturn(user("John Doe", "john@example.com",
                LocalDateTime.of(2024, Month.FEBRUARY, 1, 8, 0)));

        ResponseEntity<Void> response = userController.activateUser(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();

        verify(userService).activateUser(userId);
    }

    @Test
    void shouldDeactivateUser() {
        UUID userId = UUID.randomUUID();
        when(userService.deactivateUser(userId)).thenReturn(user("John Doe", "john@example.com",
                LocalDateTime.of(2024, Month.FEBRUARY, 1, 8, 0)));

        ResponseEntity<Void> response = userController.deactivateUser(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();

        verify(userService).deactivateUser(userId);
    }

    @Test
    void shouldSearchUsers() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2024, Month.JANUARY, 15, 11, 0);
        User user = user("John Doe", "john@example.com", createdAt);
        user.setId(userId);
        UserResponse mappedUser = UserMapper.toResponse(user);
        Pagination<UserResponse> pagination = new Pagination<>(1, 10, 1, List.of(mappedUser));

        when(userService.searchUsers(any(SearchQuery.class))).thenReturn(pagination);

        mockMvc.perform(get("/api/users/search")
                        .param("page", "1")
                        .param("perPage", "10")
                        .param("terms", "john")
                        .param("sort", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.items[0].id").value(userId.toString()))
                .andExpect(jsonPath("$.pagination.items[0].name").value("John Doe"))
                .andExpect(jsonPath("$.pagination.total").value(1))
                .andExpect(jsonPath("$.itemsCount").value(1))
                .andExpect(jsonPath("$.executionTimeMs").isNumber());

        verify(userService).searchUsers(any(SearchQuery.class));
    }

    @Test
    void shouldVerifyEmail() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "verify-token";
        LocalDateTime verifiedAt = LocalDateTime.of(2024, Month.JANUARY, 18, 18, 30);
        User user = user("John Doe", "john@example.com", verifiedAt);
        user.setId(userId);
        user.setEmailVerifiedAt(verifiedAt);

        when(userService.verifyEmail(userId, token)).thenReturn(user);

        mockMvc.perform(post("/api/users/verify-email")
                        .param("userId", userId.toString())
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.verifiedAt").value(verifiedAt.toString()));

        verify(userService).verifyEmail(userId, token);
    }

    @Test
    void shouldResendVerificationEmail() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user("Jane Smith", "jane@example.com",
                LocalDateTime.of(2024, Month.FEBRUARY, 2, 14, 15));
        user.setId(userId);

        when(userService.resendVerificationEmail(userId)).thenReturn(user);

        mockMvc.perform(post("/api/users/{id}/resend-verification", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.tokenSent").value(true))
                .andExpect(jsonPath("$.message").value("Token de verificação reenviado para jane@example.com"));

        verify(userService).resendVerificationEmail(userId);
    }

    @Test
    void shouldAssignRolesToUser() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    UUID permissionId = UUID.randomUUID();

    UserRoleAssignmentRequest request = UserRoleAssignmentRequest.builder()
        .roleIds(List.of(roleId))
        .build();

    UserAssignmentService.AssignmentResult assignmentResult = new UserAssignmentService.AssignmentResult(
        userId,
        tenantId,
        List.of(roleId),
        List.of(roleId),
        List.of(),
        List.of(),
        List.of(permissionId),
        List.of(),
        List.of(permissionId)
    );

    when(userAssignmentService.assignRoles(userId, tenantId, List.of(roleId))).thenReturn(assignmentResult);
    when(usersTenantsRolesService.getUserTenantRolesDetails(userId, tenantId)).thenReturn(List.of(
        Map.of("id", roleId, "name", "ADMIN", "description", "Administrador")
    ));
    when(usersTenantsRolesService.countRolesByUserAndTenant(userId, tenantId)).thenReturn(1L);
    when(usersTenantsPermissionsService.getUserTenantPermissionsDetails(userId, tenantId)).thenReturn(List.of(
        Map.of("id", permissionId, "action", "manage", "resource", "users")
    ));
    when(usersTenantsPermissionsService.countPermissionsByUserAndTenant(userId, tenantId)).thenReturn(1L);

    mockMvc.perform(post("/api/users/{id}/tenants/{tenantId}/roles", userId, tenantId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
        .andExpect(jsonPath("$.requestedRoleIds[0]").value(roleId.toString()))
        .andExpect(jsonPath("$.newlyAssignedRoleIds[0]").value(roleId.toString()))
        .andExpect(jsonPath("$.propagatedPermissionIds[0]").value(permissionId.toString()))
        .andExpect(jsonPath("$.tenantRoles[0].name").value("ADMIN"))
        .andExpect(jsonPath("$.tenantPermissions[0].action").value("manage"))
        .andExpect(jsonPath("$.totalRolesForUserInTenant").value(1))
        .andExpect(jsonPath("$.totalDirectPermissionsForUserInTenant").value(1));

    verify(userAssignmentService).assignRoles(userId, tenantId, List.of(roleId));
    verify(usersTenantsRolesService).getUserTenantRolesDetails(userId, tenantId);
    verify(usersTenantsPermissionsService).getUserTenantPermissionsDetails(userId, tenantId);
    }

    @Test
    void shouldGetUserSessions() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime creation = LocalDateTime.of(2024, Month.FEBRUARY, 5, 9, 15);
        LocalDateTime expiration = creation.plusHours(4);
        Session session = Session.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .userAgent("JUnit")
                .ipAddress(java.net.InetAddress.getByName("127.0.0.1"))
                .createdAt(creation)
                .expiresAt(expiration)
                .refreshTokenHash("hash")
                .build();

        when(sessionService.findActiveSessionsByUser(userId)).thenReturn(List.of(session));

        mockMvc.perform(get("/api/users/{id}/sessions", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(session.getId().toString()))
                .andExpect(jsonPath("$[0].ipAddress").value("127.0.0.1"))
                .andExpect(jsonPath("$[0].userAgent").value("JUnit"))
                .andExpect(jsonPath("$[0].createdAt").value(creation.toString()))
                .andExpect(jsonPath("$[0].expiresAt").value(expiration.toString()));

        verify(sessionService).findActiveSessionsByUser(userId);
    }

    private User user(String name, String email, LocalDateTime timestamp) {
        User user = User.createNew(name, email, "hash");
        user.setCreatedAt(timestamp);
        user.setUpdatedAt(timestamp);
        return user;
    }
}
