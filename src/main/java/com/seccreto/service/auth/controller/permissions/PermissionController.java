package com.seccreto.service.auth.controller.permissions;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.common.SearchResponse;
import com.seccreto.service.auth.api.dto.permissions.PermissionPolicyPresetResponse;
import com.seccreto.service.auth.api.dto.permissions.PermissionRequest;
import com.seccreto.service.auth.api.dto.permissions.PermissionResponse;
import com.seccreto.service.auth.api.dto.permissions.PermissionUpdateRequest;
import com.seccreto.service.auth.api.mapper.permissions.PermissionMapper;
import com.seccreto.service.auth.config.RequirePermission;
import com.seccreto.service.auth.config.RequireRole;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.permissions.PermissionService;
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
@RequestMapping("/api/permissions")
@Tag(name = "Gestão de Permissões", description = "Endpoints para administração de permissões por landlord")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "Criar permissão", description = "Cria uma nova permissão para um landlord")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Permissão criada com sucesso",
                    content = @Content(schema = @Schema(implementation = PermissionResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ação e recurso já estão em uso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
        @PostMapping
        @RequireRole({"SUPER_ADMIN", "ADMIN"})
        @RequirePermission("create:permissions")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody PermissionRequest request) {
        Permission permission = permissionService.createPermission(
                request.getLandlordId(),
                request.getAction(),
                request.getResource()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PermissionMapper.toResponse(permission));
    }

    @Operation(summary = "Listar permissões", description = "Lista permissões associadas a um landlord")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de permissões retornada",
                    content = @Content(schema = @Schema(implementation = PermissionResponse.class)))
    })
        @GetMapping
        @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
        @RequirePermission("read:permissions")
    public ResponseEntity<List<PermissionResponse>> listPermissions(
            @Parameter(description = "ID do landlord") @RequestParam UUID landlordId) {
        List<Permission> permissions = permissionService.listPermissions(landlordId);
        return ResponseEntity.ok(PermissionMapper.toResponseList(permissions));
    }

    @Operation(summary = "Buscar permissões", description = "Busca permissões por ação ou recurso com paginação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class)))
    })
        @GetMapping("/search")
        @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
        @RequirePermission("read:permissions")
    public ResponseEntity<SearchResponse<PermissionResponse>> searchPermissions(
            @Parameter(description = "ID do landlord") @RequestParam UUID landlordId,
            @Parameter(description = "Página atual") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Itens por página") @RequestParam(defaultValue = "10") int perPage,
            @Parameter(description = "Termos de busca") @RequestParam(required = false) String terms,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "action") String sort,
            @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "asc") String direction) {

        long startTime = System.currentTimeMillis();

        SearchQuery searchQuery = new SearchQuery(page, perPage, terms, sort, direction);
        Pagination<PermissionResponse> pagination = permissionService.searchPermissions(landlordId, searchQuery);

        long executionTime = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(SearchResponse.of(pagination, executionTime));
    }

    @Operation(summary = "Buscar permissão por ID", description = "Obtém detalhes de uma permissão específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permissão encontrada",
                    content = @Content(schema = @Schema(implementation = PermissionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Permissão não encontrada")
    })
        @GetMapping("/{id}")
        @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
        @RequirePermission("read:permissions")
    public ResponseEntity<PermissionResponse> getPermissionById(
            @Parameter(description = "ID do landlord") @RequestParam UUID landlordId,
            @Parameter(description = "ID da permissão") @PathVariable UUID id) {
        return permissionService.findPermissionById(landlordId, id)
                .map(PermissionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar permissão", description = "Atualiza parcialmente uma permissão existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permissão atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = PermissionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Permissão não encontrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
        @PatchMapping("/{id}")
        @RequireRole({"SUPER_ADMIN", "ADMIN"})
        @RequirePermission("update:permissions")
    public ResponseEntity<PermissionResponse> updatePermission(
            @Parameter(description = "ID do landlord") @RequestParam UUID landlordId,
            @Parameter(description = "ID da permissão") @PathVariable UUID id,
            @Valid @RequestBody PermissionUpdateRequest request) {

        if (request.getAction() == null && request.getResource() == null) {
            throw new ValidationException("Pelo menos ação ou recurso devem ser informados para atualização");
        }

        Permission current = permissionService.findPermissionById(landlordId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada para o landlord informado"));

        String action = request.getAction() != null ? request.getAction() : current.getAction();
        String resource = request.getResource() != null ? request.getResource() : current.getResource();

        Permission updated = permissionService.updatePermission(landlordId, id, action, resource);
        return ResponseEntity.ok(PermissionMapper.toResponse(updated));
    }

    @Operation(summary = "Remover permissão", description = "Remove uma permissão de um landlord")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Permissão removida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Permissão não encontrada")
    })
        @DeleteMapping("/{id}")
        @RequireRole({"SUPER_ADMIN", "ADMIN"})
        @RequirePermission("delete:permissions")
    public ResponseEntity<Void> deletePermission(
            @Parameter(description = "ID do landlord") @RequestParam UUID landlordId,
            @Parameter(description = "ID da permissão") @PathVariable UUID id) {
        permissionService.deletePermission(landlordId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Catálogo fixo de políticas", description = "Retorna a lista de políticas pré-definidas disponíveis para associação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catálogo retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = PermissionPolicyPresetResponse.class)))
    })
        @GetMapping("/policy-presets")
        @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
        @RequirePermission("read:permissions")
    public ResponseEntity<List<PermissionPolicyPresetResponse>> listPolicyPresets() {
        return ResponseEntity.ok(permissionService.listPolicyPresets());
    }
}
