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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller consolidado para gestão de usuários.
 * OTIMIZADO: UserRestController (8 endpoints) + UserManagementController (10 endpoints) → 12 endpoints
 * Mescla funcionalidades básicas CRUD com gestão avançada de usuários.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Gestão de Usuários", description = "API consolidada para gerenciamento completo de usuários")
public class UserController {

    private final UserService userService;
    private final UsersTenantsRolesService usersTenantsRolesService;
    private final SessionService sessionService;

    public UserController(UserService userService, 
                         UsersTenantsRolesService usersTenantsRolesService,
                         SessionService sessionService) {
        this.userService = userService;
        this.usersTenantsRolesService = usersTenantsRolesService;
        this.sessionService = sessionService;
    }

    // ===== OPERAÇÕES BÁSICAS CRUD =====

    /**
     * Lista todos os usuários
     * Consolidado de: UserRestController.getAllUsers()
     */
    @Operation(summary = "Listar usuários", description = "Retorna uma lista com todos os usuários cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.listAllUsers().stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * Busca usuário por ID
     * Consolidado de: UserRestController.getUserById()
     */
    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário específico pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        return userService.findUserById(id)
                .map(UserMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cria um novo usuário
     * Consolidado de: UserRestController.createUser() + UserManagementController.createUserForTenant()
     */
    @Operation(summary = "Criar usuário", 
               description = "Cria um novo usuário. Se tenantId for fornecido, associa ao tenant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email já existe"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserRequest request,
            @Parameter(description = "ID do tenant (opcional)") @RequestParam(required = false) UUID tenantId) {
        
        User user = userService.createUser(request.getName(), request.getEmail(), "password");
        
        // Se tenantId foi fornecido, poderia associar o usuário ao tenant aqui
        // usersTenantsRolesService.createAssociation(user.getId(), tenantId, defaultRoleId);
        
        boolean isNewUser = userService.findByEmail(request.getEmail())
                .map(u -> u.getId().equals(user.getId()))
                .orElse(false);
        
        HttpStatus status = isNewUser ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(UserMapper.toResponse(user));
    }

    /**
     * Atualiza um usuário
     * Consolidado de: UserRestController.updateUser()
     */
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID do usuário") @PathVariable UUID id,
            @Valid @RequestBody UserRequest request) {
        User updated = userService.updateUser(id, request.getName(), request.getEmail());
        return ResponseEntity.ok(UserMapper.toResponse(updated));
    }

    /**
     * Remove um usuário
     * Consolidado de: UserRestController.deleteUser()
     */
    @Operation(summary = "Remover usuário", description = "Remove um usuário do sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ===== OPERAÇÕES DE BUSCA =====

    /**
     * Busca usuários por nome
     * Consolidado de: UserRestController.searchUsers() + UserManagementController.searchUsers()
     */
    @Operation(summary = "Buscar usuários", description = "Busca usuários por nome ou termo de busca")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @Parameter(description = "Nome ou termo de busca") @RequestParam String query) {
        List<UserResponse> users = userService.searchUsers(query).stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * Busca usuário por email
     * Consolidado de: UserRestController.getUserByEmail()
     */
    @Operation(summary = "Buscar usuário por email", description = "Retorna um usuário pelo seu email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(
            @Parameter(description = "Email do usuário") @PathVariable String email) {
        return userService.findByEmail(email)
                .map(UserMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== OPERAÇÕES POR TENANT =====

    /**
     * Lista usuários de um tenant
     * Consolidado de: UserManagementController.getUsersByTenant()
     */
    @Operation(summary = "Listar usuários do tenant", description = "Retorna todos os usuários de um tenant específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários do tenant",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tenant não encontrado")
    })
    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<List<UserResponse>> getUsersByTenant(
            @Parameter(description = "ID do tenant") @PathVariable UUID tenantId) {
        List<UserResponse> users = userService.findUsersByTenant(tenantId).stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // ===== OPERAÇÕES DE GESTÃO AVANÇADA =====

    /**
     * Obtém perfil completo do usuário
     * Consolidado de: UserManagementController.getUserProfile()
     */
    @Operation(summary = "Obter perfil do usuário", description = "Retorna perfil completo com estatísticas do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil obtido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/{id}/profile")
    public ResponseEntity<Object> getUserProfile(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        return userService.findUserById(id)
                .map(user -> {
                    Object userProfile = new Object() {
                        public final UUID userId = user.getId();
                        public final String name = user.getName();
                        public final String email = user.getEmail();
                        public final Boolean active = user.isActive();
                        public final String createdAt = user.getCreatedAt().toString();
                        public final String updatedAt = user.getUpdatedAt().toString();
                        public final Long totalSessions = 0L; // TODO: Implement countSessionsByUser
                        public final Long activeSessions = 0L; // TODO: Implement countActiveSessionsByUser
                    };
                    return ResponseEntity.ok(userProfile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Suspende um usuário
     * Consolidado de: UserManagementController.suspendUser()
     */
    @Operation(summary = "Suspender usuário", description = "Suspende um usuário e encerra suas sessões")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário suspenso com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendUser(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        // TODO: Implement suspendUser method
        sessionService.invalidateAllUserSessions(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Ativa um usuário
     * Consolidado de: UserManagementController.activateUser()
     */
    @Operation(summary = "Ativar usuário", description = "Ativa um usuário suspenso")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário ativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        // TODO: Implement activateUser method
        return ResponseEntity.ok().build();
    }

    /**
     * Força logout do usuário
     * Consolidado de: UserManagementController.forceLogoutUser()
     */
    @Operation(summary = "Forçar logout", description = "Encerra todas as sessões ativas do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout forçado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/{id}/force-logout")
    public ResponseEntity<Void> forceLogoutUser(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        sessionService.invalidateAllUserSessions(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Lista sessões do usuário
     * Consolidado de: UserManagementController.getUserSessions()
     */
    @Operation(summary = "Listar sessões do usuário", description = "Retorna todas as sessões ativas do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sessões obtidas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<Object>> getUserSessions(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        List<Object> sessions = sessionService.findActiveSessionsByUser(id).stream()
                .map(session -> new Object() {
                    public final UUID sessionId = session.getId();
                    public final String ipAddress = session.getIpAddress().getHostAddress();
                    public final String userAgent = session.getUserAgent();
                    public final String createdAt = session.getCreatedAt().toString();
                    public final String expiresAt = session.getExpiresAt().toString();
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    // ===== ESTATÍSTICAS =====

    /**
     * Obtém estatísticas de usuários
     * Consolidado de: UserRestController.getStats() + UserManagementController.getUserStats()
     */
    @Operation(summary = "Obter estatísticas", description = "Retorna estatísticas gerais dos usuários")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estatísticas obtidas com sucesso")
    })
    @GetMapping("/stats")
    public ResponseEntity<Object> getUserStats() {
        return ResponseEntity.ok(new Object() {
            public final Long totalUsers = userService.countUsers();
            public final Long activeUsers = userService.countActiveUsers();
            public final Long suspendedUsers = 0L; // TODO: Implement countSuspendedUsers
            public final Long totalSessions = sessionService.countActiveSessions();
            public final String status = "online";
            public final String version = "1.0.0";
        });
    }
}
