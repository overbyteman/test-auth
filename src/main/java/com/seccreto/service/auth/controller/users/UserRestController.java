package com.seccreto.service.auth.controller.users;

import com.seccreto.service.auth.api.dto.users.UserRequest;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.api.mapper.users.UserMapper;
import com.seccreto.service.auth.model.users.User;
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
import java.util.stream.Collectors;

/**
 * Controlador REST para API (Controller) refatorado aplicando SOLID.
 * - SRP: Apenas coordena requisições HTTP.
 * - DIP: Depende da abstração UserService.
 * - ISP: Exposição somente do necessário via DTOs.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários", description = "API para gerenciamento de usuários")
public class UserRestController {

    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    /** Lista todos os usuários */
    @Operation(summary = "Listar usuários", description = "Retorna uma lista com todos os usuários cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.listAllUsers().stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /** Busca usuário por ID */
    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário específico pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        return userService.findUserById(id)
                .map(UserMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Cria um novo usuário (idempotente) */
    @Operation(summary = "Criar usuário", description = "Cria um novo usuário no sistema. Idempotente: se usuário já existe, retorna o existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "200", description = "Usuário já existe (idempotência)",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserRequest request) {
        User user = userService.createUser(request.getName(), request.getEmail());
        
        // Verificar se é um usuário novo ou existente para determinar o status code
        boolean isNewUser = userService.findByEmail(request.getEmail())
                .map(u -> u.getId().equals(user.getId()))
                .orElse(false);
        
        HttpStatus status = isNewUser ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(UserMapper.toResponse(user));
    }

    /** Atualiza um usuário (idempotente) */
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário existente. Idempotente: se dados são iguais, retorna sem alterações.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado ou sem alterações (idempotência)",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "409", description = "Email já existe")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        User updated = userService.updateUser(id, request.getName(), request.getEmail());
        return ResponseEntity.ok(UserMapper.toResponse(updated));
    }

    /** Remove um usuário (idempotente) */
    @Operation(summary = "Remover usuário", description = "Remove um usuário do sistema. Idempotente: se usuário não existe, retorna 204.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário removido ou já não existe (idempotência)")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        userService.deleteUser(id); // Sempre retorna 204 (idempotente)
        return ResponseEntity.noContent().build();
    }

    /** Busca usuários por nome */
    @Operation(summary = "Buscar usuários por nome", description = "Busca usuários que contenham o nome especificado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetro inválido")
    })
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @Parameter(description = "Nome ou parte do nome") @RequestParam String name) {
        List<UserResponse> users = userService.findUsersByName(name).stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /** Busca usuário por email */
    @Operation(summary = "Buscar usuário por email", description = "Busca um usuário pelo seu email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(
            @Parameter(description = "Email do usuário") @PathVariable String email) {
        return userService.findByEmail(email)
                .map(UserMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Estatísticas do sistema */
    @Operation(summary = "Estatísticas", description = "Retorna estatísticas gerais do sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estatísticas retornadas")
    })
    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        long userCount = userService.countUsers();
        return ResponseEntity.ok(new Object() {
            public final long totalUsers = userCount;
            public final String status = "online";
            public final String version = "1.0.0";
        });
    }
}
