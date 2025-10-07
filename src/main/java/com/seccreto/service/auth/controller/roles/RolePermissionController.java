package com.seccreto.service.auth.controller.roles;

import com.seccreto.service.auth.api.dto.roles_permissions.RolePermissionAttachRequest;
import com.seccreto.service.auth.api.dto.roles_permissions.RolePermissionPolicyUpdateRequest;
import com.seccreto.service.auth.api.dto.roles_permissions.RolesPermissionsResponse;
import com.seccreto.service.auth.api.mapper.roles_permissions.RolesPermissionsMapper;
import com.seccreto.service.auth.config.RequirePermission;
import com.seccreto.service.auth.config.RequireRole;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.service.rolespermissions.RolePermissionService;
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

@RestController
@RequestMapping("/api/roles/{roleId}/permissions")
@Tag(name = "Associação de Roles e Permissões", description = "Endpoints para relacionar roles a permissões com policies dedicadas")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @PostMapping
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:permissions")
    @Operation(summary = "Anexar permissão a role", description = "Vincula uma permissão a um role, permitindo definir uma policy específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Permissão vinculada",
                    content = @Content(schema = @Schema(implementation = RolesPermissionsResponse.class)))
    })
    public ResponseEntity<RolesPermissionsResponse> attachPermission(
            @Parameter(description = "ID do landlord", required = true)
            @RequestParam UUID landlordId,
            @Parameter(description = "ID do role", required = true)
            @PathVariable UUID roleId,
            @Valid @RequestBody RolePermissionAttachRequest request) {

        boolean inheritPolicy = request.getInheritPermissionPolicy() == null || request.getInheritPermissionPolicy();
        RolesPermissions association = rolePermissionService.attachPermission(
                landlordId,
                roleId,
                request.getPermissionId(),
                request.getPolicyId(),
                inheritPolicy
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RolesPermissionsMapper.toResponse(association));
    }

    @PatchMapping("/{permissionId}/policy")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:permissions")
    @Operation(summary = "Atualizar policy da associação", description = "Define ou altera a policy aplicada entre um role e uma permissão")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Policy atualizada",
                    content = @Content(schema = @Schema(implementation = RolesPermissionsResponse.class)))
    })
    public ResponseEntity<RolesPermissionsResponse> updatePolicy(
            @Parameter(description = "ID do landlord", required = true)
            @RequestParam UUID landlordId,
            @Parameter(description = "ID do role", required = true)
            @PathVariable UUID roleId,
            @Parameter(description = "ID da permissão", required = true)
            @PathVariable UUID permissionId,
            @Valid @RequestBody RolePermissionPolicyUpdateRequest request) {

        boolean inheritPolicy = request.getInheritPermissionPolicy() == null || request.getInheritPermissionPolicy();
        RolesPermissions association = rolePermissionService.updatePermissionPolicy(
                landlordId,
                roleId,
                permissionId,
                request.getPolicyId(),
                inheritPolicy
        );

        return ResponseEntity.ok(RolesPermissionsMapper.toResponse(association));
    }

    @DeleteMapping("/{permissionId}")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:permissions")
    @Operation(summary = "Remover permissão de role", description = "Desvincula uma permissão de um role, removendo qualquer policy associada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Permissão removida"),
            @ApiResponse(responseCode = "404", description = "Associação inexistente")
    })
    public ResponseEntity<Void> detachPermission(
            @Parameter(description = "ID do landlord", required = true)
            @RequestParam UUID landlordId,
            @Parameter(description = "ID do role", required = true)
            @PathVariable UUID roleId,
            @Parameter(description = "ID da permissão", required = true)
            @PathVariable UUID permissionId) {

        boolean removed = rolePermissionService.detachPermission(landlordId, roleId, permissionId);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    @RequirePermission("read:permissions")
    @Operation(summary = "Listar permissões de um role", description = "Retorna as permissões associadas a um role e suas policies efetivas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de permissões",
                    content = @Content(schema = @Schema(implementation = RolesPermissionsResponse.class)))
    })
    public ResponseEntity<List<RolesPermissionsResponse>> listRolePermissions(
            @Parameter(description = "ID do landlord", required = true)
            @RequestParam UUID landlordId,
            @Parameter(description = "ID do role", required = true)
            @PathVariable UUID roleId) {

        List<RolesPermissions> associations = rolePermissionService.listRolePermissions(landlordId, roleId);
        return ResponseEntity.ok(RolesPermissionsMapper.toResponseList(associations));
    }
}
