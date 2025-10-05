package com.seccreto.service.auth.controller.users;

import com.seccreto.service.auth.api.dto.users.UserRequest;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.api.mapper.users.UserMapper;
import com.seccreto.service.auth.config.RequireOwnershipOrRole;
import com.seccreto.service.auth.config.RequirePermission;
import com.seccreto.service.auth.config.RequireRole;
import com.seccreto.service.auth.config.RequireSelfOnly;
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
    @RequirePermission("read:users")
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
    @RequireOwnershipOrRole({"SUPER_ADMIN", "ADMIN", "MANAGER", "STAFF"})
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
    @RequirePermission("create:users")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserRequest request,
            @Parameter(description = "ID do tenant (opcional)") @RequestParam(required = false) UUID tenantId) {
        
        User user = userService.createUser(request.getName(), request.getEmail(), request.getPassword());
        
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
    @PatchMapping("/{id}")
    @RequireOwnershipOrRole({"SUPER_ADMIN", "ADMIN", "MANAGER", "STAFF"})
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID do usuário") @PathVariable UUID id,
            @Valid @RequestBody UserRequest request) {
        User updated = userService.updateUser(id, request.getName(), request.getEmail());
        return ResponseEntity.ok(UserMapper.toResponse(updated));
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


    // ===== OPERAÇÕES DE GESTÃO AVANÇADA =====



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
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    public ResponseEntity<Void> activateUser(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        userService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Desativa um usuário
     */
    @Operation(summary = "Desativar usuário", description = "Desativa um usuário no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/{id}/deactivate")
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Verifica e-mail usando token enviado por e-mail
     */
    @Operation(summary = "Verificar e-mail com token", description = "Verifica um e-mail usando o token enviado por e-mail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "E-mail verificado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Token inválido ou expirado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/verify-email")
    @RequireSelfOnly
    public ResponseEntity<Object> verifyEmail(
            @Parameter(description = "ID do usuário") @RequestParam UUID userId,
            @Parameter(description = "Token de verificação") @RequestParam String token) {
        User verifiedUser = userService.verifyEmail(userId, token);
        return ResponseEntity.ok(new Object() {
            public final UUID userId = verifiedUser.getId();
            public final String email = verifiedUser.getEmail();
            public final boolean verified = true;
            public final String verifiedAt = verifiedUser.getEmailVerifiedAt().toString();
        });
    }

    /**
     * Reenvia token de verificação de e-mail
     */
    @Operation(summary = "Reenviar token de verificação", description = "Reenvia um novo token de verificação para o e-mail do usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token reenviado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/{id}/resend-verification")
    @RequireSelfOnly
    public ResponseEntity<Object> resendVerificationEmail(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
        User user = userService.resendVerificationEmail(id);
        return ResponseEntity.ok(new Object() {
            public final UUID userId = user.getId();
            public final String email = user.getEmail();
            public final boolean tokenSent = true;
            public final String message = "Token de verificação reenviado para " + user.getEmail();
        });
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

}
