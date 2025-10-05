package com.seccreto.service.auth.controller.tenant;

import com.seccreto.service.auth.api.dto.landlords.LandlordRequest;
import com.seccreto.service.auth.api.dto.landlords.LandlordResponse;
import com.seccreto.service.auth.api.mapper.landlords.LandlordMapper;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.service.landlords.LandlordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/landlords")
@Tag(name = "Gestão de Landlords", description = "Endpoints para administração dos proprietários de tenants")
public class LandlordManagementController {

    private final LandlordService landlordService;

    public LandlordManagementController(LandlordService landlordService) {
        this.landlordService = landlordService;
    }

    @Operation(summary = "Criar landlord")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Landlord criado",
                    content = @Content(schema = @Schema(implementation = LandlordResponse.class)))
    })
    @PostMapping
    public ResponseEntity<LandlordResponse> create(@Valid @RequestBody LandlordRequest request) {
        Landlord landlord = landlordService.createLandlord(request.getName(), request.getConfig());
        return ResponseEntity.status(HttpStatus.CREATED).body(LandlordMapper.toResponse(landlord));
    }

    @Operation(summary = "Listar landlords")
    @GetMapping
    public ResponseEntity<List<LandlordResponse>> list() {
        List<LandlordResponse> responses = landlordService.listAllLandlords().stream()
                .map(LandlordMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Atualizar landlord")
    @PutMapping("/{id}")
    public ResponseEntity<LandlordResponse> update(@PathVariable UUID id,
                                                   @Valid @RequestBody LandlordRequest request) {
        Landlord landlord = landlordService.updateLandlord(id, request.getName(), request.getConfig());
        return ResponseEntity.ok(LandlordMapper.toResponse(landlord));
    }

    @Operation(summary = "Remover landlord")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        landlordService.deleteLandlord(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obter landlord por ID")
    @GetMapping("/{id}")
    public ResponseEntity<LandlordResponse> get(@PathVariable UUID id) {
        return landlordService.findLandlordById(id)
                .map(LandlordMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
