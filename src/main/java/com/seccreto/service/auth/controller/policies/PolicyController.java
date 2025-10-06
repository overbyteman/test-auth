package com.seccreto.service.auth.controller.policies;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.common.SearchResponse;
import com.seccreto.service.auth.api.dto.policies.PolicyRequest;
import com.seccreto.service.auth.api.dto.policies.PolicyResponse;
import com.seccreto.service.auth.api.mapper.policies.PolicyMapper;
import com.seccreto.service.auth.config.RequirePermission;
import com.seccreto.service.auth.config.RequireRole;
import com.seccreto.service.auth.config.RequireTenantAccess;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.service.policies.PolicyService;
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
 * Controller semântico para gestão de políticas ABAC.
 * Endpoints baseados em casos de uso reais de administração de políticas.
 */
@RestController
@RequestMapping("/api/policy-management")
@Tag(name = "Gestão de Políticas ABAC", description = "Endpoints semânticos para administração de políticas")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    /**
     * CASO DE USO: Administrador cria nova política
     * Endpoint semântico: POST /api/policy-management/policies
     */
    @Operation(
        summary = "Criar nova política", 
        description = "Cria uma nova política ABAC no sistema (apenas administradores)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Política criada com sucesso",
                content = @Content(schema = @Schema(implementation = PolicyResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nome da política já existe"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/policies")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("create:policies")
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody PolicyRequest request) {
        Policy policy = policyService.createPolicy(
            request.getTenantId(),
            request.getCode(),
            request.getName(), 
            request.getDescription(), 
            request.getEffect(), 
            request.getActions(),
            request.getResources(),
            request.getConditions()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyMapper.toResponse(policy));
    }

    /**
     * CASO DE USO: Administrador lista todas as políticas
     * Endpoint semântico: GET /api/policy-management/policies
     */
    @Operation(
        summary = "Listar todas as políticas", 
        description = "Retorna lista de todas as políticas do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de políticas retornada",
                content = @Content(schema = @Schema(implementation = PolicyResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/policies")
    @RequirePermission("read:policies")
    @RequireTenantAccess
    public ResponseEntity<List<PolicyResponse>> getAllPolicies(@RequestParam("tenantId") UUID tenantId) {
        List<PolicyResponse> policies = policyService.listPolicies(tenantId).stream()
                .map(PolicyMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(policies);
    }

    /**
     * CASO DE USO: Administrador obtém detalhes de uma política
     * Endpoint semântico: GET /api/policy-management/policies/{id}
     */
    @Operation(
        summary = "Obter detalhes da política", 
        description = "Retorna informações detalhadas de uma política específica"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalhes da política",
                content = @Content(schema = @Schema(implementation = PolicyResponse.class))),
        @ApiResponse(responseCode = "404", description = "Política não encontrada"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/policies/{id}")
    @RequirePermission("read:policies")
    @RequireTenantAccess
    public ResponseEntity<PolicyResponse> getPolicyDetails(
        @RequestParam("tenantId") UUID tenantId,
        @Parameter(description = "ID da política") @PathVariable UUID id) {
        return policyService.findPolicyById(tenantId, id)
                .map(PolicyMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * CASO DE USO: Administrador atualiza política
     * Endpoint semântico: PUT /api/policy-management/policies/{id}
     */
    @Operation(
        summary = "Atualizar política", 
        description = "Atualiza uma política existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Política atualizada com sucesso",
                content = @Content(schema = @Schema(implementation = PolicyResponse.class))),
        @ApiResponse(responseCode = "404", description = "Política não encontrada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PatchMapping("/policies/{id}")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("update:policies")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @Parameter(description = "ID da política") @PathVariable UUID id,
            @Valid @RequestBody PolicyRequest request) {
        Policy policy = policyService.updatePolicy(
            request.getTenantId(),
            id, 
            request.getName(), 
            request.getDescription(), 
            request.getEffect(), 
            request.getActions(),
            request.getResources(),
            request.getConditions()
        );
        return ResponseEntity.ok(PolicyMapper.toResponse(policy));
    }

    /**
     * CASO DE USO: Administrador desativa política
     * Endpoint semântico: DELETE /api/policy-management/policies/{id}
     */
    @Operation(
        summary = "Desativar política", 
        description = "Desativa uma política (soft delete) - não será mais aplicada"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Política desativada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Política não encontrada"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @DeleteMapping("/policies/{id}")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("delete:policies")
    @RequireTenantAccess
    public ResponseEntity<Void> deactivatePolicy(
            @RequestParam("tenantId") UUID tenantId,
            @Parameter(description = "ID da política") @PathVariable UUID id) {
        policyService.deletePolicy(tenantId, id);
        return ResponseEntity.noContent().build();
    }



    /**
     * CASO DE USO: Administrador busca políticas por critério
     * Endpoint semântico: GET /api/policy-management/policies/search
     */
    @Operation(
        summary = "Buscar políticas", 
        description = "Busca políticas por nome, descrição ou efeito com paginação"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Busca realizada",
                content = @Content(schema = @Schema(implementation = SearchResponse.class))),
        @ApiResponse(responseCode = "400", description = "Critério de busca inválido"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/policies/search")
    @RequirePermission("read:policies")
    @RequireTenantAccess
    public ResponseEntity<SearchResponse<PolicyResponse>> searchPolicies(
            @RequestParam("tenantId") UUID tenantId,
            @Parameter(description = "Página atual") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Itens por página") @RequestParam(defaultValue = "10") int perPage,
            @Parameter(description = "Termos de busca") @RequestParam(required = false) String terms,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "name") String sort,
            @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "asc") String direction) {
        
        long startTime = System.currentTimeMillis();
        
        SearchQuery searchQuery = new SearchQuery(page, perPage, terms, sort, direction);
        Pagination<PolicyResponse> pagination = policyService.searchPolicies(tenantId, searchQuery);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.ok(SearchResponse.of(pagination, executionTime));
    }



    // ===== DTOs INTERNOS =====
    
    public static class PolicyEvaluationRequest {
        private UUID userId;
        private Object context;
        
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public Object getContext() { return context; }
        public void setContext(Object context) { this.context = context; }
    }
}
