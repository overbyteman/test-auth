package com.seccreto.service.auth.controller.setup;

import com.seccreto.service.auth.api.dto.landlords.LandlordResponse;
import com.seccreto.service.auth.api.dto.tenants.TenantRequest;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.api.mapper.landlords.LandlordMapper;
import com.seccreto.service.auth.config.RequirePermission;
import com.seccreto.service.auth.config.RequireRole;
import com.seccreto.service.auth.service.landlords.LandlordService;
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
    @PostMapping("/network/{landlordId}/roles")
    @RequireRole({"SUPER_ADMIN", "ADMIN"})
    @RequirePermission("manage:setup")
    public ResponseEntity<Object> setupDefaultRoles(
            @Parameter(description = "ID do landlord") @PathVariable UUID landlordId) {
        
        long startTime = System.currentTimeMillis();
        
        int rolesCreated = setupService.setupDefaultRolesForLandlord(landlordId);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
    return ResponseEntity.ok(
        java.util.Map.of(
            "message", "Roles padrões configurados com sucesso",
            "rolesCreatedCount", rolesCreated,
            "executionTimeMs", executionTime
        )
    );
    }

    /**
     * CASO DE USO: Listar todas as redes (landlords)
     * Endpoint: GET /api/setup/networks
     */
    @Operation(
        summary = "Listar redes",
        description = "Lista todas as redes de academias cadastradas"
    )
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
}
