package com.seccreto.service.auth.controller.roles;

import com.seccreto.service.auth.api.dto.roles.RoleRequest;
import com.seccreto.service.auth.api.dto.roles.RoleResponse;
import com.seccreto.service.auth.api.dto.permissions.PermissionRequest;
import com.seccreto.service.auth.api.dto.permissions.PermissionResponse;
import com.seccreto.service.auth.api.dto.roles_permissions.RolesPermissionsRequest;
import com.seccreto.service.auth.api.dto.roles_permissions.RolesPermissionsResponse;
import com.seccreto.service.auth.api.mapper.roles.RoleMapper;
import com.seccreto.service.auth.api.mapper.permissions.PermissionMapper;
import com.seccreto.service.auth.api.mapper.roles_permissions.RolesPermissionsMapper;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.service.roles.RoleService;
import com.seccreto.service.auth.service.permissions.PermissionService;
import com.seccreto.service.auth.service.roles_permissions.RolesPermissionsService;
import com.seccreto.service.auth.service.users_tenants_roles.UsersTenantsRolesService;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller semântico para gestão de roles e permissões.
 * Endpoints baseados em casos de uso reais de administração de permissões.
 */
@RestController
@RequestMapping("/api/role-permission-management")
@Tag(name = "Gestão de Roles e Permissões", description = "Endpoints semânticos para administração de roles e permissões")
public class RolePermissionController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RolesPermissionsService rolesPermissionsService;
    private final UsersTenantsRolesService usersTenantsRolesService;

    public RolePermissionController(RoleService roleService, 
                                  PermissionService permissionService,
                                  RolesPermissionsService rolesPermissionsService,
                                  UsersTenantsRolesService usersTenantsRolesService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.rolesPermissionsService = rolesPermissionsService;
        this.usersTenantsRolesService = usersTenantsRolesService;
    }

    // ===== GESTÃO DE ROLES =====

    /**
     * CASO DE USO: Administrador cria novo role
     * Endpoint semântico: POST /api/role-permission-management/roles
     */
    @Operation(
        summary = "Criar novo role", 
        description = "Cria um novo role no sistema (apenas administradores)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Role criado com sucesso",
                content = @Content(schema = @Schema(implementation = RoleResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nome do role já existe"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        Role role = roleService.createRole(request.getName(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleMapper.toResponse(role));
    }

    /**
     * CASO DE USO: Administrador lista todos os roles
     * Endpoint semântico: GET /api/role-permission-management/roles
     */
    @Operation(
        summary = "Listar todos os roles", 
        description = "Retorna lista de todos os roles do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de roles retornada",
                content = @Content(schema = @Schema(implementation = RoleResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> roles = roleService.listAllRoles().stream()
                .map(RoleMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    /**
     * CASO DE USO: Administrador obtém detalhes de um role
     * Endpoint semântico: GET /api/role-permission-management/roles/{id}
     */
    @Operation(
        summary = "Obter detalhes do role", 
        description = "Retorna informações detalhadas de um role específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalhes do role",
                content = @Content(schema = @Schema(implementation = RoleResponse.class))),
        @ApiResponse(responseCode = "404", description = "Role não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/roles/{id}")
    public ResponseEntity<RoleResponse> getRoleDetails(
            @Parameter(description = "ID do role") @PathVariable Long id) {
        return roleService.findRoleById(id)
                .map(RoleMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== GESTÃO DE PERMISSÕES =====

    /**
     * CASO DE USO: Administrador cria nova permissão
     * Endpoint semântico: POST /api/role-permission-management/permissions
     */
    @Operation(
        summary = "Criar nova permissão", 
        description = "Cria uma nova permissão no sistema (apenas administradores)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Permissão criada com sucesso",
                content = @Content(schema = @Schema(implementation = PermissionResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nome da permissão já existe"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/permissions")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody PermissionRequest request) {
        Permission permission = permissionService.createPermission(request.getAction(), request.getResource());
        return ResponseEntity.status(HttpStatus.CREATED).body(PermissionMapper.toResponse(permission));
    }

    /**
     * CASO DE USO: Administrador lista todas as permissões
     * Endpoint semântico: GET /api/role-permission-management/permissions
     */
    @Operation(
        summary = "Listar todas as permissões", 
        description = "Retorna lista de todas as permissões do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de permissões retornada",
                content = @Content(schema = @Schema(implementation = PermissionResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        List<PermissionResponse> permissions = permissionService.listAllPermissions().stream()
                .map(PermissionMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissions);
    }

    // ===== ASSOCIAÇÃO DE ROLES E PERMISSÕES =====

    /**
     * CASO DE USO: Administrador associa permissão a role
     * Endpoint semântico: POST /api/role-permission-management/roles/{roleId}/permissions
     */
    @Operation(
        summary = "Associar permissão a role", 
        description = "Associa uma permissão a um role específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Permissão associada com sucesso",
                content = @Content(schema = @Schema(implementation = RolesPermissionsResponse.class))),
        @ApiResponse(responseCode = "404", description = "Role ou permissão não encontrado"),
        @ApiResponse(responseCode = "409", description = "Associação já existe"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<RolesPermissionsResponse> assignPermissionToRole(
            @Parameter(description = "ID do role") @PathVariable Long roleId,
            @Valid @RequestBody RolesPermissionsRequest request) {
        RolesPermissions association = rolesPermissionsService.createAssociation(roleId, request.getPermissionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(RolesPermissionsMapper.toResponse(association));
    }

    /**
     * CASO DE USO: Administrador remove permissão de role
     * Endpoint semântico: DELETE /api/role-permission-management/roles/{roleId}/permissions/{permissionId}
     */
    @Operation(
        summary = "Remover permissão de role", 
        description = "Remove uma permissão de um role específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Permissão removida com sucesso"),
        @ApiResponse(responseCode = "404", description = "Associação não encontrada"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> removePermissionFromRole(
            @Parameter(description = "ID do role") @PathVariable Long roleId,
            @Parameter(description = "ID da permissão") @PathVariable Long permissionId) {
        rolesPermissionsService.removeAssociation(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * CASO DE USO: Administrador lista permissões de um role
     * Endpoint semântico: GET /api/role-permission-management/roles/{roleId}/permissions
     */
    @Operation(
        summary = "Listar permissões do role", 
        description = "Retorna lista de permissões associadas a um role"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permissões do role",
                content = @Content(schema = @Schema(implementation = PermissionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Role não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<List<PermissionResponse>> getRolePermissions(
            @Parameter(description = "ID do role") @PathVariable Long roleId) {
        List<PermissionResponse> permissions = rolesPermissionsService.findPermissionsByRole(roleId).stream()
                .map(rolePermission -> permissionService.findPermissionById(rolePermission.getPermissionId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(PermissionMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissions);
    }

    // ===== GESTÃO DE USUÁRIOS E ROLES =====

    /**
     * CASO DE USO: Administrador atribui role a usuário em tenant
     * Endpoint semântico: POST /api/role-permission-management/users/{userId}/tenants/{tenantId}/roles
     */
    @Operation(
        summary = "Atribuir role a usuário", 
        description = "Atribui um role a um usuário em um tenant específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Role atribuído com sucesso"),
        @ApiResponse(responseCode = "404", description = "Usuário, tenant ou role não encontrado"),
        @ApiResponse(responseCode = "409", description = "Atribuição já existe"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/users/{userId}/tenants/{tenantId}/roles")
    public ResponseEntity<Void> assignRoleToUser(
            @Parameter(description = "ID do usuário") @PathVariable Long userId,
            @Parameter(description = "ID do tenant") @PathVariable Long tenantId,
            @Valid @RequestBody AssignRoleRequest request) {
        usersTenantsRolesService.createAssociation(userId, tenantId, request.getRoleId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * CASO DE USO: Administrador remove role de usuário
     * Endpoint semântico: DELETE /api/role-permission-management/users/{userId}/tenants/{tenantId}/roles/{roleId}
     */
    @Operation(
        summary = "Remover role de usuário", 
        description = "Remove um role de um usuário em um tenant específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Role removido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Atribuição não encontrada"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @DeleteMapping("/users/{userId}/tenants/{tenantId}/roles/{roleId}")
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "ID do usuário") @PathVariable Long userId,
            @Parameter(description = "ID do tenant") @PathVariable Long tenantId,
            @Parameter(description = "ID do role") @PathVariable Long roleId) {
        usersTenantsRolesService.removeAssociation(userId, tenantId, roleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * CASO DE USO: Sistema verifica permissões do usuário
     * Endpoint semântico: GET /api/role-permission-management/users/{userId}/permissions
     */
    @Operation(
        summary = "Verificar permissões do usuário", 
        description = "Retorna todas as permissões de um usuário em todos os tenants"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permissões obtidas"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<Object> getUserPermissions(
            @Parameter(description = "ID do usuário") @PathVariable Long userId) {
        final Long userIdFinal = userId;
        return ResponseEntity.ok(new Object() {
            public final Long userId = userIdFinal;
            public final List<String> roles = usersTenantsRolesService.findRoleNamesByUser(userIdFinal);
            public final List<String> permissions = usersTenantsRolesService.findPermissionNamesByUser(userIdFinal);
            public final Long totalRoles = usersTenantsRolesService.countRolesByUser(userIdFinal);
            public final Long totalPermissions = usersTenantsRolesService.countPermissionsByUser(userIdFinal);
        });
    }

    /**
     * CASO DE USO: Sistema verifica saúde da gestão de roles e permissões
     * Endpoint semântico: GET /api/role-permission-management/health
     */
    @Operation(
        summary = "Verificar saúde da gestão de roles e permissões", 
        description = "Retorna status do sistema de gestão de roles e permissões"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sistema funcionando normalmente")
    })
    @GetMapping("/health")
    public ResponseEntity<Object> getRolePermissionHealth() {
        return ResponseEntity.ok(new Object() {
            public final String status = "healthy";
            public final String service = "role-permission-management";
            public final Long totalRoles = roleService.countRoles();
            public final Long totalPermissions = permissionService.countPermissions();
            public final Long totalAssociations = rolesPermissionsService.countAssociations();
        });
    }

    // ===== DTO INTERNO =====
    
    public static class AssignRoleRequest {
        private Long roleId;
        
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
    }
}
