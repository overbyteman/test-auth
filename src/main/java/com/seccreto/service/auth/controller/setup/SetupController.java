package com.seccreto.service.auth.controller.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seccreto.service.auth.api.dto.common.SearchResponse;
import com.seccreto.service.auth.api.dto.landlords.LandlordRequest;
import com.seccreto.service.auth.api.dto.landlords.LandlordResponse;
import com.seccreto.service.auth.api.dto.tenants.TenantRequest;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.api.mapper.landlords.LandlordMapper;
import com.seccreto.service.auth.api.mapper.tenants.TenantMapper;
import com.seccreto.service.auth.config.RequirePermission;
import com.seccreto.service.auth.config.RequireRole;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.service.landlords.LandlordService;
import com.seccreto.service.auth.service.tenants.TenantService;
import com.seccreto.service.auth.service.setup.SetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller para setup e instalação de novas redes de academias.
 * 
 * Este controller é responsável por:
 * - Criar novos landlords (matrizes)
 * - Configurar roles padrões para academias de luta
 * - Criar tenants (filiais) para o landlord
 * - Configurar permissões e políticas
 */
@RestController
@RequestMapping("/api/setup")
@Tag(name = "Setup de Rede", description = "Endpoints para instalação e configuração de novas redes de academias")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;
    private final LandlordService landlordService;
    private final TenantService tenantService;
    private final ObjectMapper objectMapper;

    /**
     * CASO DE USO: Criar nova rede de academias (Landlord + Roles)
     * Endpoint: POST /api/setup/network
     */
    @Operation(
        summary = "Criar nova rede de academias", 
        description = "Cria um novo landlord (matriz) com todos os roles padrões para academias de luta"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Rede criada com sucesso",
                content = @Content(schema = @Schema(implementation = LandlordResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nome da rede já existe"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping("/network")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:setup")
    public ResponseEntity<LandlordResponse> createNetwork(@Valid @RequestBody NetworkSetupRequest request) {
        long startTime = System.currentTimeMillis();
        
        LandlordResponse landlord = setupService.createNetworkWithRoles(
            request.getName(),
            request.getDescription(),
            request.getConfig()
        );
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .header("X-Execution-Time", String.valueOf(executionTime))
            .body(landlord);
    }

    /**
     * CASO DE USO: Adicionar nova filial (Tenant) à rede
     * Endpoint: POST /api/setup/network/{landlordId}/tenant
     */
    @Operation(
        summary = "Adicionar filial à rede", 
        description = "Cria uma nova filial (tenant) para um landlord existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Filial criada com sucesso",
                content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @ApiResponse(responseCode = "404", description = "Landlord não encontrado"),
        @ApiResponse(responseCode = "409", description = "Nome da filial já existe"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping("/network/{landlordId}/tenant")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:setup")
    public ResponseEntity<TenantResponse> addTenantToNetwork(
            @Parameter(description = "ID do landlord") @PathVariable UUID landlordId,
            @Valid @RequestBody TenantRequest request) {
        
        long startTime = System.currentTimeMillis();
        
        TenantResponse tenant = setupService.addTenantToNetwork(landlordId, request);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .header("X-Execution-Time", String.valueOf(executionTime))
            .body(tenant);
    }

    /**
     * CASO DE USO: Configurar roles padrões para um landlord existente
     * Endpoint: POST /api/setup/network/{landlordId}/roles
     */
    @Operation(
        summary = "Configurar roles padrões", 
        description = "Adiciona todos os roles padrões para academias de luta a um landlord existente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Roles configurados com sucesso"),
        @ApiResponse(responseCode = "404", description = "Landlord não encontrado"),
        @ApiResponse(responseCode = "409", description = "Roles já existem para este landlord")
    })
    @PostMapping("/network/{landlordId}/roles")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:setup")
    public ResponseEntity<Object> setupDefaultRoles(
            @Parameter(description = "ID do landlord") @PathVariable UUID landlordId) {
        
        long startTime = System.currentTimeMillis();
        
        int rolesCreated = setupService.setupDefaultRolesForLandlord(landlordId);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.ok(new Object() {
            public final String message = "Roles padrões configurados com sucesso";
            public final int rolesCreatedCount = rolesCreated;
            public final long executionTimeMs = executionTime;
        });
    }

    /**
     * CASO DE USO: Verificar status de uma rede
     * Endpoint: GET /api/setup/network/{landlordId}/status
     */
    @Operation(
        summary = "Verificar status da rede", 
        description = "Retorna informações sobre o status de configuração de uma rede"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status obtido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Landlord não encontrado")
    })
    @GetMapping("/network/{landlordId}/status")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:setup")
    public ResponseEntity<Object> getNetworkStatus(
            @Parameter(description = "ID do landlord") @PathVariable UUID landlordId) {
        
        long startTime = System.currentTimeMillis();
        
        SetupService.NetworkStatus status = setupService.getNetworkStatus(landlordId);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.ok(new Object() {
            public final SetupService.NetworkStatus networkStatus = status;
            public final long executionTimeMs = executionTime;
        });
    }

    /**
     * CASO DE USO: Listar todas as redes (landlords)
     * Endpoint: GET /api/setup/networks
     */
    @Operation(
        summary = "Listar redes", 
        description = "Lista todas as redes de academias cadastradas"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de redes retornada")
    })
    @GetMapping("/networks")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:setup")
    public ResponseEntity<List<LandlordResponse>> listNetworks() {
        long startTime = System.currentTimeMillis();
        
        List<LandlordResponse> networks = landlordService.listAllLandlords().stream()
            .map(LandlordMapper::toResponse)
            .toList();
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.ok()
            .header("X-Execution-Time", String.valueOf(executionTime))
            .body(networks);
    }

    /**
     * CASO DE USO: Setup completo de uma nova rede
     * Endpoint: POST /api/setup/network/complete
     */
    @Operation(
        summary = "Setup completo de rede", 
        description = "Cria uma rede completa com landlord, roles padrões e filial inicial"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Rede completa criada com sucesso"),
        @ApiResponse(responseCode = "409", description = "Nome da rede já existe"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping("/network/complete")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:setup")
    public ResponseEntity<Object> createCompleteNetwork(@Valid @RequestBody CompleteNetworkRequest request) {
        long startTime = System.currentTimeMillis();
        
        SetupService.CompleteNetworkRequest serviceRequest = new SetupService.CompleteNetworkRequest();
        serviceRequest.setNetworkName(request.getNetworkName());
        serviceRequest.setNetworkDescription(request.getNetworkDescription());
        serviceRequest.setFirstTenantName(request.getFirstTenantName());
        serviceRequest.setNetworkConfig(request.getNetworkConfig());
        serviceRequest.setTenantConfig(request.getTenantConfig());
        
        SetupService.CompleteNetworkResponse response = setupService.createCompleteNetwork(serviceRequest);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .header("X-Execution-Time", String.valueOf(executionTime))
            .body(response);
    }

    // ===== DTOs INTERNOS =====
    
    public static class NetworkSetupRequest {
        private String name;
        private String description;
        private JsonNode config;
        
        // Getters e Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public JsonNode getConfig() { return config; }
        public void setConfig(JsonNode config) { this.config = config; }
    }

    public static class NetworkStatus {
        private boolean hasRoles;
        private boolean hasPolicies;
        private boolean hasPermissions;
        private int rolesCount;
        private int tenantsCount;
        private int policiesCount;
        private int permissionsCount;
        
        // Getters e Setters
        public boolean isHasRoles() { return hasRoles; }
        public void setHasRoles(boolean hasRoles) { this.hasRoles = hasRoles; }
        
        public boolean isHasPolicies() { return hasPolicies; }
        public void setHasPolicies(boolean hasPolicies) { this.hasPolicies = hasPolicies; }
        
        public boolean isHasPermissions() { return hasPermissions; }
        public void setHasPermissions(boolean hasPermissions) { this.hasPermissions = hasPermissions; }
        
        public int getRolesCount() { return rolesCount; }
        public void setRolesCount(int rolesCount) { this.rolesCount = rolesCount; }
        
        public int getTenantsCount() { return tenantsCount; }
        public void setTenantsCount(int tenantsCount) { this.tenantsCount = tenantsCount; }
        
        public int getPoliciesCount() { return policiesCount; }
        public void setPoliciesCount(int policiesCount) { this.policiesCount = policiesCount; }
        
        public int getPermissionsCount() { return permissionsCount; }
        public void setPermissionsCount(int permissionsCount) { this.permissionsCount = permissionsCount; }
    }

    public static class CompleteNetworkRequest {
        private String networkName;
        private String networkDescription;
        private String firstTenantName;
        private JsonNode networkConfig;
        private JsonNode tenantConfig;
        
        // Getters e Setters
        public String getNetworkName() { return networkName; }
        public void setNetworkName(String networkName) { this.networkName = networkName; }
        
        public String getNetworkDescription() { return networkDescription; }
        public void setNetworkDescription(String networkDescription) { this.networkDescription = networkDescription; }
        
        public String getFirstTenantName() { return firstTenantName; }
        public void setFirstTenantName(String firstTenantName) { this.firstTenantName = firstTenantName; }
        
        public JsonNode getNetworkConfig() { return networkConfig; }
        public void setNetworkConfig(JsonNode networkConfig) { this.networkConfig = networkConfig; }
        
        public JsonNode getTenantConfig() { return tenantConfig; }
        public void setTenantConfig(JsonNode tenantConfig) { this.tenantConfig = tenantConfig; }
    }

    public static class CompleteNetworkResponse {
        private LandlordResponse landlord;
        private TenantResponse firstTenant;
        private int rolesCreated;
        private int policiesCreated;
        private int permissionsCreated;
        private long totalExecutionTime;
        
        // Getters e Setters
        public LandlordResponse getLandlord() { return landlord; }
        public void setLandlord(LandlordResponse landlord) { this.landlord = landlord; }
        
        public TenantResponse getFirstTenant() { return firstTenant; }
        public void setFirstTenant(TenantResponse firstTenant) { this.firstTenant = firstTenant; }
        
        public int getRolesCreated() { return rolesCreated; }
        public void setRolesCreated(int rolesCreated) { this.rolesCreated = rolesCreated; }
        
        public int getPoliciesCreated() { return policiesCreated; }
        public void setPoliciesCreated(int policiesCreated) { this.policiesCreated = policiesCreated; }
        
        public int getPermissionsCreated() { return permissionsCreated; }
        public void setPermissionsCreated(int permissionsCreated) { this.permissionsCreated = permissionsCreated; }
        
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public void setTotalExecutionTime(long totalExecutionTime) { this.totalExecutionTime = totalExecutionTime; }
    }
}
