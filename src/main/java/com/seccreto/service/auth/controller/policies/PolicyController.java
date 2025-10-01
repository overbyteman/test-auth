package com.seccreto.service.auth.controller.policies;

import com.seccreto.service.auth.api.dto.policies.PolicyRequest;
import com.seccreto.service.auth.api.dto.policies.PolicyResponse;
import com.seccreto.service.auth.api.mapper.policies.PolicyMapper;
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
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody PolicyRequest request) {
        Policy policy = policyService.createPolicy(
            request.getName(), 
            request.getDescription(), 
            request.getEffect(), 
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
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        List<PolicyResponse> policies = policyService.listAllPolicies().stream()
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
    public ResponseEntity<PolicyResponse> getPolicyDetails(
            @Parameter(description = "ID da política") @PathVariable Long id) {
        return policyService.findPolicyById(id)
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
    @PutMapping("/policies/{id}")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @Parameter(description = "ID da política") @PathVariable Long id,
            @Valid @RequestBody PolicyRequest request) {
        Policy policy = policyService.updatePolicy(
            id, 
            request.getName(), 
            request.getDescription(), 
            request.getEffect(), 
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
    public ResponseEntity<Void> deactivatePolicy(
            @Parameter(description = "ID da política") @PathVariable Long id) {
        policyService.deactivatePolicy(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * CASO DE USO: Administrador reativa política
     * Endpoint semântico: POST /api/policy-management/policies/{id}/activate
     */
    @Operation(
        summary = "Reativar política", 
        description = "Reativa uma política previamente desativada"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Política reativada com sucesso",
                content = @Content(schema = @Schema(implementation = PolicyResponse.class))),
        @ApiResponse(responseCode = "404", description = "Política não encontrada"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/policies/{id}/activate")
    public ResponseEntity<PolicyResponse> activatePolicy(
            @Parameter(description = "ID da política") @PathVariable Long id) {
        Policy policy = policyService.activatePolicy(id);
        return ResponseEntity.ok(PolicyMapper.toResponse(policy));
    }

    /**
     * CASO DE USO: Sistema avalia política para usuário
     * Endpoint semântico: POST /api/policy-management/policies/{id}/evaluate
     */
    @Operation(
        summary = "Avaliar política", 
        description = "Avalia se uma política se aplica a um usuário específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Avaliação realizada"),
        @ApiResponse(responseCode = "404", description = "Política não encontrada"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/policies/{id}/evaluate")
    public ResponseEntity<Object> evaluatePolicy(
            @Parameter(description = "ID da política") @PathVariable Long id,
            @Valid @RequestBody PolicyEvaluationRequest request) {
        Boolean appliesResult = policyService.evaluatePolicy(id, request.getUserId(), request.getContext());
        return ResponseEntity.ok(new Object() {
            public final Long policyId = id;
            public final Long userId = request.getUserId();
            public final Boolean applies = appliesResult;
            public final String result = appliesResult ? "ALLOW" : "DENY";
        });
    }

    /**
     * CASO DE USO: Administrador busca políticas por critério
     * Endpoint semântico: GET /api/policy-management/policies/search
     */
    @Operation(
        summary = "Buscar políticas", 
        description = "Busca políticas por nome, descrição ou efeito"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Busca realizada",
                content = @Content(schema = @Schema(implementation = PolicyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Critério de busca inválido"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/policies/search")
    public ResponseEntity<List<PolicyResponse>> searchPolicies(
            @Parameter(description = "Termo de busca") @RequestParam String query) {
        List<PolicyResponse> policies = policyService.searchPolicies(query).stream()
                .map(PolicyMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(policies);
    }

    /**
     * CASO DE USO: Administrador obtém estatísticas de políticas
     * Endpoint semântico: GET /api/policy-management/policies/stats
     */
    @Operation(
        summary = "Obter estatísticas de políticas", 
        description = "Retorna estatísticas gerais das políticas do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estatísticas obtidas"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/policies/stats")
    public ResponseEntity<Object> getPolicyStats() {
        return ResponseEntity.ok(new Object() {
            public final Long totalPolicies = policyService.countPolicies();
            public final Long activePolicies = policyService.countActivePolicies();
            public final Long allowPolicies = policyService.countPoliciesByEffect("ALLOW");
            public final Long denyPolicies = policyService.countPoliciesByEffect("DENY");
        });
    }

    /**
     * CASO DE USO: Sistema verifica saúde da gestão de políticas
     * Endpoint semântico: GET /api/policy-management/health
     */
    @Operation(
        summary = "Verificar saúde da gestão de políticas", 
        description = "Retorna status do sistema de gestão de políticas"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sistema funcionando normalmente")
    })
    @GetMapping("/health")
    public ResponseEntity<Object> getPolicyManagementHealth() {
        return ResponseEntity.ok(new Object() {
            public final String status = "healthy";
            public final String service = "policy-management";
            public final Long totalPolicies = policyService.countPolicies();
            public final Long activePolicies = policyService.countActivePolicies();
        });
    }

    // ===== DTOs INTERNOS =====
    
    public static class PolicyEvaluationRequest {
        private Long userId;
        private Object context;
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Object getContext() { return context; }
        public void setContext(Object context) { this.context = context; }
    }
}
