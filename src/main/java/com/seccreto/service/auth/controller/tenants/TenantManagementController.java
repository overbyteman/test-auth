package com.seccreto.service.auth.controller.tenants;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.common.SearchResponse;
import com.seccreto.service.auth.api.dto.tenants.TenantRequest;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.api.mapper.tenants.TenantMapper;
import com.seccreto.service.auth.config.RequirePermission;
import com.seccreto.service.auth.config.RequireRole;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.service.tenants.TenantService;
import com.seccreto.service.auth.service.users.UserService;
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
 * Controller semântico para gestão de tenants.
 * Endpoints baseados em casos de uso reais de administração de tenants.
 */
@RestController
@RequestMapping("/api/tenant-management")
@Tag(name = "Gestão de Tenants", description = "Endpoints semânticos para administração de tenants")
public class TenantManagementController {

    private final TenantService tenantService;
    private final UserService userService;

    public TenantManagementController(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    /**
     * CASO DE USO: Administrador cria novo tenant
     * Endpoint semântico: POST /api/tenant-management/tenants
     */
    @Operation(
        summary = "Criar novo tenant", 
        description = "Cria um novo tenant no sistema (apenas administradores)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tenant criado com sucesso",
                content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nome do tenant já existe"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/tenants")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("create:tenants")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody TenantRequest request) {
        Tenant tenant = tenantService.createTenant(
            request.getName(),
            request.getConfig(),
            request.getLandlordId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantMapper.toResponse(tenant));
    }

    /**
     * CASO DE USO: Administrador lista todos os tenants
     * Endpoint semântico: GET /api/tenant-management/tenants
     */
    @Operation(
        summary = "Listar todos os tenants", 
        description = "Retorna lista de todos os tenants do sistema (apenas administradores)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de tenants retornada",
                content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/tenants")
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    @RequirePermission("read:tenants")
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        List<TenantResponse> tenants = tenantService.listAllTenants().stream()
                .map(TenantMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tenants);
    }

    /**
     * CASO DE USO: Administrador obtém detalhes de um tenant
     * Endpoint semântico: GET /api/tenant-management/tenants/{id}
     */
    @Operation(
        summary = "Obter detalhes do tenant", 
        description = "Retorna informações detalhadas de um tenant específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalhes do tenant",
                content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tenant não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/tenants/{id}")
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    @RequirePermission("read:tenants")
    public ResponseEntity<TenantResponse> getTenantDetails(
            @Parameter(description = "ID do tenant") @PathVariable UUID id) {
        return tenantService.findTenantById(id)
                .map(TenantMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * CASO DE USO: Administrador atualiza configurações do tenant
     * Endpoint semântico: PUT /api/tenant-management/tenants/{id}
     */
    @Operation(
        summary = "Atualizar tenant", 
        description = "Atualiza as configurações de um tenant existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tenant atualizado com sucesso",
                content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tenant não encontrado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PatchMapping("/tenants/{id}")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("update:tenants")
    public ResponseEntity<TenantResponse> updateTenant(
            @Parameter(description = "ID do tenant") @PathVariable UUID id,
            @Valid @RequestBody TenantRequest request) {
    Tenant tenant = tenantService.updateTenant(id, request.getName(), request.getConfig(), request.getLandlordId());
        return ResponseEntity.ok(TenantMapper.toResponse(tenant));
    }

    /**
     * CASO DE USO: Administrador desativa tenant
     * Endpoint semântico: DELETE /api/tenant-management/tenants/{id}
     */
    @Operation(
        summary = "Desativar tenant", 
        description = "Desativa um tenant (soft delete) - usuários não poderão mais acessar"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tenant desativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Tenant não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @DeleteMapping("/tenants/{id}")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("delete:tenants")
    public ResponseEntity<Void> deactivateTenant(
            @Parameter(description = "ID do tenant") @PathVariable UUID id) {
        tenantService.deactivateTenant(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * CASO DE USO: Administrador reativa tenant
     * Endpoint semântico: POST /api/tenant-management/tenants/{id}/activate
     */
    @Operation(
        summary = "Reativar tenant", 
        description = "Reativa um tenant previamente desativado"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tenant reativado com sucesso",
                content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tenant não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/tenants/{id}/activate")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("update:tenants")
    public ResponseEntity<TenantResponse> activateTenant(
            @Parameter(description = "ID do tenant") @PathVariable UUID id) {
        Tenant tenant = tenantService.activateTenant(id);
        return ResponseEntity.ok(TenantMapper.toResponse(tenant));
    }


    /**
     * CASO DE USO: Administrador busca tenants por nome
     * Endpoint semântico: GET /api/tenant-management/tenants/search
     */
    @Operation(
        summary = "Buscar tenants", 
        description = "Busca tenants por nome ou domínio com paginação"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Busca realizada",
                content = @Content(schema = @Schema(implementation = SearchResponse.class))),
        @ApiResponse(responseCode = "400", description = "Parâmetro de busca inválido"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/tenants/search")
    @RequireRole({"SUPER_ADMIN", "ADMIN", "MANAGER"})
    @RequirePermission("read:tenants")
    public ResponseEntity<SearchResponse<TenantResponse>> searchTenants(
            @Parameter(description = "Página atual") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Itens por página") @RequestParam(defaultValue = "10") int perPage,
            @Parameter(description = "Termos de busca") @RequestParam(required = false) String terms,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "name") String sort,
            @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "asc") String direction) {
        
        long startTime = System.currentTimeMillis();
        
        SearchQuery searchQuery = new SearchQuery(page, perPage, terms, sort, direction);
        Pagination<TenantResponse> pagination = tenantService.searchTenants(searchQuery);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.ok(SearchResponse.of(pagination, executionTime));
    }

    /**
     * CASO DE USO: Sistema verifica saúde da gestão de tenants
     * Endpoint semântico: GET /api/tenant-management/health
     */
    @Operation(
        summary = "Verificar saúde da gestão de tenants", 
        description = "Retorna status do sistema de gestão de tenants"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sistema funcionando normalmente")
    })
    @GetMapping("/health")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("read:tenants")
    public ResponseEntity<Object> getTenantManagementHealth() {
        return ResponseEntity.ok(new Object() {
            public final String status = "healthy";
            public final String service = "tenant-management";
            public final Long totalTenants = tenantService.countTenants();
            public final Long activeTenants = 0L; // TODO: Implement countActiveTenants method
        });
    }
}
