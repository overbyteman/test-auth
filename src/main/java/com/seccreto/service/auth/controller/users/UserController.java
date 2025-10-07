package com.seccreto.service.auth.controller.users;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.common.SearchResponse;
import com.seccreto.service.auth.api.dto.users.AssignedPermissionDetail;
import com.seccreto.service.auth.api.dto.users.AssignedRoleDetail;
import com.seccreto.service.auth.api.dto.users.UserAssignmentResponse;
import com.seccreto.service.auth.api.dto.users.UserRequest;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.api.dto.users.UserRoleAssignmentRequest;
import com.seccreto.service.auth.api.mapper.users.UserMapper;
import com.seccreto.service.auth.config.RequireOwnershipOrRole;
import com.seccreto.service.auth.config.RequirePermission;
import com.seccreto.service.auth.config.RequireRole;
import com.seccreto.service.auth.config.RequireSelfOnly;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.users.assignments.UserAssignmentService;
import com.seccreto.service.auth.service.users_tenants_permissions.UsersTenantsPermissionsService;
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
import java.util.LinkedHashMap;
import java.util.Map;
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
        private final UsersTenantsPermissionsService usersTenantsPermissionsService;
        private final SessionService sessionService;
        private final UserAssignmentService userAssignmentService;

        public UserController(UserService userService,
                                                  UsersTenantsRolesService usersTenantsRolesService,
                                                  UsersTenantsPermissionsService usersTenantsPermissionsService,
                                                  SessionService sessionService,
                                                  UserAssignmentService userAssignmentService) {
                this.userService = userService;
                this.usersTenantsRolesService = usersTenantsRolesService;
                this.usersTenantsPermissionsService = usersTenantsPermissionsService;
                this.sessionService = sessionService;
                this.userAssignmentService = userAssignmentService;
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
        @PutMapping("/{id}")
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
    @Operation(summary = "Buscar usuários", description = "Busca usuários por nome ou termo de busca com paginação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class)))
    })
    @GetMapping("/search")
        @RequirePermission("read:users")
    public ResponseEntity<SearchResponse<UserResponse>> searchUsers(
            @Parameter(description = "Página atual") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Itens por página") @RequestParam(defaultValue = "10") int perPage,
            @Parameter(description = "Termos de busca") @RequestParam(required = false) String terms,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "name") String sort,
            @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "asc") String direction) {
        
        long startTime = System.currentTimeMillis();
        
        SearchQuery searchQuery = new SearchQuery(page, perPage, terms, sort, direction);
        Pagination<UserResponse> pagination = userService.searchUsers(searchQuery);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.ok(SearchResponse.of(pagination, executionTime));
    }


    // ===== OPERAÇÕES DE GESTÃO AVANÇADA =====

    /**
     * Atribui roles a um usuário em um tenant, propagando as permissões correspondentes.
     */
    @Operation(summary = "Atribuir roles a usuário",
            description = "Atribui roles a um usuário em um tenant específico e propaga as permissões dos roles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles atribuídos com sucesso",
                    content = @Content(schema = @Schema(implementation = UserAssignmentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário ou tenant não encontrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping("/{id}/tenants/{tenantId}/roles")
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    @RequirePermission("manage:users")
    public ResponseEntity<UserAssignmentResponse> assignRolesToUser(
            @Parameter(description = "ID do usuário") @PathVariable UUID id,
            @Parameter(description = "ID do tenant") @PathVariable UUID tenantId,
            @Valid @RequestBody UserRoleAssignmentRequest request) {

        UserAssignmentService.AssignmentResult result = userAssignmentService.assignRoles(id, tenantId, request.getRoleIds());
        UserAssignmentResponse response = buildAssignmentResponse(result,
                "Roles atribuídos com sucesso e permissões propagadas.");
        return ResponseEntity.ok(response);
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
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("userId", verifiedUser.getId());
                payload.put("email", verifiedUser.getEmail());
                payload.put("verified", true);
                payload.put("verifiedAt", verifiedUser.getEmailVerifiedAt() != null ? verifiedUser.getEmailVerifiedAt().toString() : null);
                return ResponseEntity.ok(payload);
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
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("userId", user.getId());
                payload.put("email", user.getEmail());
                payload.put("tokenSent", true);
                payload.put("message", "Token de verificação reenviado para " + user.getEmail());
                return ResponseEntity.ok(payload);
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
    @RequireOwnershipOrRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    @RequirePermission("read:sessions")
    public ResponseEntity<List<Object>> getUserSessions(
            @Parameter(description = "ID do usuário") @PathVariable UUID id) {
                List<Object> sessions = sessionService.findActiveSessionsByUser(id).stream()
                                .map(session -> {
                                        Map<String, Object> payload = new LinkedHashMap<>();
                                        payload.put("sessionId", session.getId());
                                        payload.put("ipAddress", session.getIpAddress().getHostAddress());
                                        payload.put("userAgent", session.getUserAgent());
                                        payload.put("createdAt", session.getCreatedAt().toString());
                                        payload.put("expiresAt", session.getExpiresAt().toString());
                                        return (Object) payload;
                                })
                                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

        private UserAssignmentResponse buildAssignmentResponse(UserAssignmentService.AssignmentResult result, String message) {
                List<AssignedRoleDetail> roleDetails = usersTenantsRolesService.getUserTenantRolesDetails(result.userId(), result.tenantId()).stream()
                                .map(this::mapRoleDetail)
                                .collect(Collectors.toList());

                List<AssignedPermissionDetail> permissionDetails = usersTenantsPermissionsService.getUserTenantPermissionsDetails(result.userId(), result.tenantId()).stream()
                                .map(this::mapPermissionDetail)
                                .collect(Collectors.toList());

                long totalRoles = usersTenantsRolesService.countRolesByUserAndTenant(result.userId(), result.tenantId());
                long totalPermissions = usersTenantsPermissionsService.countPermissionsByUserAndTenant(result.userId(), result.tenantId());

                return UserAssignmentResponse.builder()
                                .userId(result.userId())
                                .tenantId(result.tenantId())
                                .requestedRoleIds(safeCopy(result.requestedRoleIds()))
                                .newlyAssignedRoleIds(safeCopy(result.newlyAssignedRoleIds()))
                                .alreadyAssignedRoleIds(safeCopy(result.alreadyAssignedRoleIds()))
                                .requestedPermissionIds(safeCopy(result.requestedPermissionIds()))
                                .newlyAssignedPermissionIds(safeCopy(result.newlyAssignedPermissionIds()))
                                .alreadyAssignedPermissionIds(safeCopy(result.alreadyAssignedPermissionIds()))
                                .propagatedPermissionIds(safeCopy(result.propagatedPermissionIds()))
                                .tenantRoles(roleDetails)
                                .tenantPermissions(permissionDetails)
                                .totalRolesForUserInTenant(totalRoles)
                                .totalDirectPermissionsForUserInTenant(totalPermissions)
                                .message(message)
                                .build();
        }

        private AssignedRoleDetail mapRoleDetail(Object rawDetail) {
                if (rawDetail instanceof Map<?, ?> map) {
                        return AssignedRoleDetail.builder()
                                        .id(toUuid(map.get("id")))
                                        .name(toStringOrNull(map.get("name")))
                                        .description(toStringOrNull(map.get("description")))
                                        .build();
                }
                return AssignedRoleDetail.builder().build();
        }

        private AssignedPermissionDetail mapPermissionDetail(Object rawDetail) {
                if (rawDetail instanceof Map<?, ?> map) {
                        return AssignedPermissionDetail.builder()
                                        .id(toUuid(map.get("id")))
                                        .action(toStringOrNull(map.get("action")))
                                        .resource(toStringOrNull(map.get("resource")))
                                        .build();
                }
                return AssignedPermissionDetail.builder().build();
        }

        private List<UUID> safeCopy(List<UUID> source) {
                return source == null ? List.of() : List.copyOf(source);
        }

        private UUID toUuid(Object value) {
                if (value instanceof UUID uuid) {
                        return uuid;
                }
                if (value instanceof String str) {
                        try {
                                return UUID.fromString(str);
                        } catch (IllegalArgumentException ignored) {
                                return null;
                        }
                }
                return null;
        }

        private String toStringOrNull(Object value) {
                return value == null ? null : value.toString();
        }

    // ===== ESTATÍSTICAS =====

}
