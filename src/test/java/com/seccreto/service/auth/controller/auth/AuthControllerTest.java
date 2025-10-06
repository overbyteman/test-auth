package com.seccreto.service.auth.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seccreto.service.auth.api.dto.auth.ChangePasswordRequest;
import com.seccreto.service.auth.api.dto.auth.ForgotPasswordRequest;
import com.seccreto.service.auth.api.dto.auth.LoginRequest;
import com.seccreto.service.auth.api.dto.auth.LoginResponse;
import com.seccreto.service.auth.api.dto.auth.LogoutRequest;
import com.seccreto.service.auth.api.dto.auth.RefreshTokenRequest;
import com.seccreto.service.auth.api.dto.auth.RefreshTokenResponse;
import com.seccreto.service.auth.api.dto.auth.RegisterRequest;
import com.seccreto.service.auth.api.dto.auth.RegisterResponse;
import com.seccreto.service.auth.service.auth.AuthService;
import com.seccreto.service.auth.service.auth.PasswordMigrationService;
import com.seccreto.service.auth.service.sessions.SessionService;
import com.seccreto.service.auth.service.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private SessionService sessionService;

    @Mock
    private UserService userService;

    @Mock
    private PasswordMigrationService passwordMigrationService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
        void shouldAuthenticateUser() throws Exception {
                // use a password that complies with PasswordStrength validator rules
                LoginRequest request = new LoginRequest("user@example.com", "Str0ng!Pwd9X");
        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .tokenType("Bearer")
                .userId(UUID.randomUUID())
                .userName("John Doe")
                .userEmail("user@example.com")
                .loginTime(LocalDateTime.now())
                .build();

        when(authService.authenticateUser(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.userEmail").value("user@example.com"));

                verify(authService).authenticateUser("user@example.com", "Str0ng!Pwd9X");
    }

    @Test
        void shouldRegisterUser() throws Exception {
                // use a password that complies with PasswordStrength validator rules
                RegisterRequest request = new RegisterRequest("John Doe", "user@example.com", "Str0ng!Pwd9X");
        RegisterResponse response = RegisterResponse.builder()
                .userId(UUID.randomUUID())
                .name("John Doe")
                .email("user@example.com")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.registerUser(anyString(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.accessToken").value("access-token"));

                verify(authService).registerUser("John Doe", "user@example.com", "Str0ng!Pwd9X");
    }

    @Test
    void shouldRefreshAccessToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(1800L)
                .userId(UUID.randomUUID())
                .build();

        when(authService.refreshAccessToken(anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        verify(authService).refreshAccessToken("refresh-token");
    }

    @Test
    void shouldLogoutUser() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setToken("access-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).logoutUser("access-token");
    }

    @Test
    void shouldChangePassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-pass");
        request.setNewPassword("new-pass");

        mockMvc.perform(patch("/api/auth/change-password")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).changePassword("Bearer token", "current-pass", "new-pass");
    }

    @Test
    void shouldSendPasswordRecoveryEmail() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).sendPasswordRecoveryEmail("user@example.com");
    }
}
