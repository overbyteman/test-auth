package com.seccreto.service.auth.controller.roles;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.common.SearchResponse;
import com.seccreto.service.auth.api.dto.roles.MyRolesResponse;
import com.seccreto.service.auth.api.dto.roles.RoleRequest;
import com.seccreto.service.auth.api.dto.roles.RoleResponse;
import com.seccreto.service.auth.api.dto.roles.RoleUpdateRequest;
import com.seccreto.service.auth.api.mapper.roles.RoleMapper;
import com.seccreto.service.auth.config.RequireRole;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.service.auth.AuthService;
import com.seccreto.service.auth.service.roles.RoleService;
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

/**
 * Controller semântico para gestão de roles.
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "Gestão de Roles", description = "Endpoints para administração de roles por landlord")
public class RoleController {

        private final RoleService roleService;
        private final AuthService authService;

        public RoleController(RoleService roleService, AuthService authService) {
        this.roleService = roleService;
                this.authService = authService;
    }

    @Operation(summary = "Listar roles", description = "Lista todos os roles associados a um landlord")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de roles retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class)))
    })
    @GetMapping
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    public ResponseEntity<List<RoleResponse>> listRoles(
            @Parameter(description = "ID do landlord") @RequestParam UUID landlordId) {
        List<Role> roles = roleService.listRoles(landlordId);
        return ResponseEntity.ok(RoleMapper.toResponseList(roles));
    }

        @Operation(summary = "Minhas roles", description = "Retorna roles e permissões do usuário autenticado, conforme mapeamento presente no token JWT")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Roles do usuário retornadas com sucesso",
                                        content = @Content(schema = @Schema(implementation = MyRolesResponse.class)))
        })
        @GetMapping("/me")
        public ResponseEntity<List<MyRolesResponse>> getMyRoles(
                        @Parameter(hidden = true) @RequestHeader("Authorization") String authorization,
                        @Parameter(description = "Filtrar por tenant específico") @RequestParam(name = "tenantId", required = false) UUID tenantId) {
                List<MyRolesResponse> responses = authService.getCurrentUserRoles(authorization, tenantId);
                return ResponseEntity.ok(responses);
        }

    @Operation(summary = "Criar role", description = "Cria um novo role para um landlord")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Role criado com sucesso",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "409", description = "Código ou nome já utilizados para o landlord"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        Role role = roleService.createRole(
                request.getLandlordId(),
                request.getCode(),
                request.getName(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleMapper.toResponse(role));
    }

    @Operation(summary = "Buscar role por ID", description = "Obtém detalhes de um role específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role encontrado",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Role não encontrado")
    })
    @GetMapping("/{id}")
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    public ResponseEntity<RoleResponse> getRoleById(
            @Parameter(description = "ID do landlord") @RequestParam UUID landlordId,
            @Parameter(description = "ID do role") @PathVariable UUID id) {
        return roleService.findRoleById(landlordId, id)
                .map(RoleMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar role", description = "Atualiza nome e descrição de um role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = RoleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Role não encontrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PutMapping("/{id}")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    public ResponseEntity<RoleResponse> updateRole(
            @Parameter(description = "ID do role") @PathVariable UUID id,
            @Valid @RequestBody RoleUpdateRequest request) {
        Role updated = roleService.updateRole(request.getLandlordId(), id, request.getName(), request.getDescription());
        return ResponseEntity.ok(RoleMapper.toResponse(updated));
    }

    @Operation(summary = "Remover role", description = "Remove um role de um landlord")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Role removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Role não encontrado")
    })
    @DeleteMapping("/{id}")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "ID do landlord") @RequestParam UUID landlordId,
            @Parameter(description = "ID do role") @PathVariable UUID id) {
        roleService.deleteRole(landlordId, id);
        return ResponseEntity.noContent().build();
    }

        @Operation(summary = "Buscar roles", description = "Busca roles por nome ou código com paginação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso",
                                        content = @Content(schema = @Schema(implementation = SearchResponse.class)))
    })
    @GetMapping("/search")
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
        public ResponseEntity<SearchResponse<RoleResponse>> searchRoles(
                        @Parameter(description = "ID do landlord") @RequestParam UUID landlordId,
                        @Parameter(description = "Página atual") @RequestParam(defaultValue = "1") int page,
                        @Parameter(description = "Itens por página") @RequestParam(defaultValue = "10") int perPage,
                        @Parameter(description = "Termos de busca") @RequestParam(required = false) String terms,
                        @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "name") String sort,
                        @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "asc") String direction) {

                long startTime = System.currentTimeMillis();

                SearchQuery searchQuery = new SearchQuery(page, perPage, terms, sort, direction);
                Pagination<RoleResponse> pagination = roleService.searchRoles(landlordId, searchQuery);

                long executionTime = System.currentTimeMillis() - startTime;

                return ResponseEntity.ok(SearchResponse.of(pagination, executionTime));
    }
}
