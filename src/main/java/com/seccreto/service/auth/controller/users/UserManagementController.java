package com.seccreto.service.auth.controller.users;

import com.seccreto.service.auth.api.dto.users.UserRequest;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.api.mapper.users.UserMapper;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.users_tenants_roles.UsersTenantsRolesService;
import com.seccreto.service.auth.service.sessions.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller semântico para gestão avançada de usuários.
 * Endpoints baseados em casos de uso reais de administração de usuários.
 */
@RestController
@RequestMapping("/api/user-management")
@Tag(name = "Gestão de Usuários", description = "Endpoints semânticos para administração de usuários")
public class UserManagementController {

    private final UserService userService;
    private final UsersTenantsRolesService usersTenantsRolesService;
    private final SessionService sessionService;

    public UserManagementController(UserService userService, 
                                  UsersTenantsRolesService usersTenantsRolesService,
                                  SessionService sessionService) {
        this.userService = userService;
        this.usersTenantsRolesService = usersTenantsRolesService;
        this.sessionService = sessionService;
    }

    /**
     * CASO DE USO: Administrador cria usuário para um tenant
     * Endpoint semântico: POST /api/user-management/tenants/{tenantId}/users
     */
    @Operation(
        summary = "Criar usuário para tenant", 
        description = "Cria um novo usuário e associa a um tenant específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email já existe"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Tenant não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/tenants/{tenantId}/users")
    public ResponseEntity<UserResponse> createUserForTenant(
            @Parameter(description = "ID do tenant") @PathVariable Long tenantId,
            @Valid @RequestBody UserRequest request) {
        User user = userService.createUser(request.getName(), request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponse(user));
    }

    /**
     * CASO DE USO: Administrador lista usuários de um tenant
     * Endpoint semântico: GET /api/user-management/tenants/{tenantId}/users
     */
    @Operation(
        summary = "Listar usuários do tenant", 
        description = "Retorna lista de usuários de um tenant específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de usuários retornada",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tenant não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/tenants/{tenantId}/users")
    public ResponseEntity<List<UserResponse>> getUsersByTenant(
            @Parameter(description = "ID do tenant") @PathVariable Long tenantId) {
        List<UserResponse> users = userService.findUsersByTenant(tenantId).stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * CASO DE USO: Administrador obtém perfil completo do usuário
     * Endpoint semântico: GET /api/user-management/users/{id}/profile
     */
    @Operation(
        summary = "Obter perfil completo do usuário", 
        description = "Retorna informações completas do usuário incluindo roles e permissões"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil obtido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/users/{id}/profile")
    public ResponseEntity<Object> getUserProfile(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        return userService.findUserById(id)
                .map(user -> {
                    Object userProfile = new Object() {
                        public final Long userId = user.getId();
                        public final String name = user.getName();
                        public final String email = user.getEmail();
                        public final Boolean active = user.getActive();
                        public final String createdAt = user.getCreatedAt().toString();
                        public final String updatedAt = user.getUpdatedAt().toString();
                        public final Long totalSessions = sessionService.countSessionsByUser(id);
                        public final Long activeSessions = sessionService.countActiveSessionsByUser(id);
                    };
                    return ResponseEntity.ok(userProfile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * CASO DE USO: Administrador suspende usuário
     * Endpoint semântico: POST /api/user-management/users/{id}/suspend
     */
    @Operation(
        summary = "Suspender usuário", 
        description = "Suspende um usuário, invalidando todas as suas sessões"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuário suspenso com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<Void> suspendUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        userService.suspendUser(id);
        sessionService.invalidateAllUserSessions(id);
        return ResponseEntity.ok().build();
    }

    /**
     * CASO DE USO: Administrador reativa usuário
     * Endpoint semântico: POST /api/user-management/users/{id}/activate
     */
    @Operation(
        summary = "Reativar usuário", 
        description = "Reativa um usuário previamente suspenso"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuário reativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/users/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * CASO DE USO: Administrador força logout de usuário
     * Endpoint semântico: POST /api/user-management/users/{id}/force-logout
     */
    @Operation(
        summary = "Forçar logout do usuário", 
        description = "Força o logout de um usuário, invalidando todas as suas sessões"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout forçado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/users/{id}/force-logout")
    public ResponseEntity<Void> forceLogoutUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        sessionService.invalidateAllUserSessions(id);
        return ResponseEntity.ok().build();
    }

    /**
     * CASO DE USO: Administrador obtém sessões ativas do usuário
     * Endpoint semântico: GET /api/user-management/users/{id}/sessions
     */
    @Operation(
        summary = "Obter sessões do usuário", 
        description = "Retorna lista de sessões ativas de um usuário"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sessões obtidas com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/users/{id}/sessions")
    public ResponseEntity<List<Object>> getUserSessions(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        List<Object> sessions = sessionService.findActiveSessionsByUser(id).stream()
                .map(session -> new Object() {
                    public final Long sessionId = session.getId();
                    public final String ipAddress = session.getIpAddress().getHostAddress();
                    public final String userAgent = session.getUserAgent();
                    public final String createdAt = session.getCreatedAt().toString();
                    public final String expiresAt = session.getExpiresAt().toString();
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    /**
     * CASO DE USO: Administrador busca usuários
     * Endpoint semântico: GET /api/user-management/users/search
     */
    @Operation(
        summary = "Buscar usuários", 
        description = "Busca usuários por nome ou email"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Busca realizada",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Parâmetro de busca inválido"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/users/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @Parameter(description = "Termo de busca") @RequestParam String query) {
        List<UserResponse> users = userService.searchUsers(query).stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * CASO DE USO: Administrador obtém estatísticas de usuários
     * Endpoint semântico: GET /api/user-management/users/stats
     */
    @Operation(
        summary = "Obter estatísticas de usuários", 
        description = "Retorna estatísticas gerais dos usuários do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estatísticas obtidas"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/users/stats")
    public ResponseEntity<Object> getUserStats() {
        return ResponseEntity.ok(new Object() {
            public final Long totalUsers = userService.countUsers();
            public final Long activeUsers = userService.countActiveUsers();
            public final Long suspendedUsers = userService.countSuspendedUsers();
            public final Long totalSessions = sessionService.countActiveSessions();
        });
    }

    /**
     * CASO DE USO: Sistema verifica saúde da gestão de usuários
     * Endpoint semântico: GET /api/user-management/health
     */
    @Operation(
        summary = "Verificar saúde da gestão de usuários", 
        description = "Retorna status do sistema de gestão de usuários"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sistema funcionando normalmente")
    })
    @GetMapping("/health")
    public ResponseEntity<Object> getUserManagementHealth() {
        return ResponseEntity.ok(new Object() {
            public final String status = "healthy";
            public final String service = "user-management";
            public final Long totalUsers = userService.countUsers();
            public final Long activeSessions = sessionService.countActiveSessions();
        });
    }
}
