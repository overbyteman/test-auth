package com.seccreto.service.auth.controller.sessions;

import com.seccreto.service.auth.api.dto.sessions.SessionResponse;
import com.seccreto.service.auth.api.mapper.sessions.SessionMapper;
import com.seccreto.service.auth.model.sessions.Session;
import com.seccreto.service.auth.service.sessions.SessionService;
import com.seccreto.service.auth.service.users.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller semântico para gestão de sessões.
 * Endpoints baseados em casos de uso reais de administração de sessões.
 */
@RestController
@RequestMapping("/api/session-management")
@Tag(name = "Gestão de Sessões", description = "Endpoints semânticos para administração de sessões")
public class SessionController {

    private final SessionService sessionService;
    private final UserService userService;

    public SessionController(SessionService sessionService, UserService userService) {
        this.sessionService = sessionService;
        this.userService = userService;
    }

    /**
     * CASO DE USO: Administrador lista todas as sessões ativas
     * Endpoint semântico: GET /api/session-management/sessions
     */
    @Operation(
        summary = "Listar sessões ativas", 
        description = "Retorna lista de todas as sessões ativas no sistema (apenas administradores)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de sessões retornada",
                content = @Content(schema = @Schema(implementation = SessionResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> getAllActiveSessions() {
        List<SessionResponse> sessions = sessionService.listActiveSessions().stream()
                .map(SessionMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    /**
     * CASO DE USO: Administrador obtém sessões de um usuário
     * Endpoint semântico: GET /api/session-management/users/{userId}/sessions
     */
    @Operation(
        summary = "Obter sessões do usuário", 
        description = "Retorna lista de sessões ativas de um usuário específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sessões do usuário",
                content = @Content(schema = @Schema(implementation = SessionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<List<SessionResponse>> getUserSessions(
            @Parameter(description = "ID do usuário") @PathVariable Long userId) {
        List<SessionResponse> sessions = sessionService.findActiveSessionsByUser(userId).stream()
                .map(SessionMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    /**
     * CASO DE USO: Administrador obtém detalhes de uma sessão
     * Endpoint semântico: GET /api/session-management/sessions/{id}
     */
    @Operation(
        summary = "Obter detalhes da sessão", 
        description = "Retorna informações detalhadas de uma sessão específica"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalhes da sessão",
                content = @Content(schema = @Schema(implementation = SessionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Sessão não encontrada"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/sessions/{id}")
    public ResponseEntity<SessionResponse> getSessionDetails(
            @Parameter(description = "ID da sessão") @PathVariable Long id) {
        return sessionService.findSessionById(id)
                .map(SessionMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * CASO DE USO: Administrador encerra sessão específica
     * Endpoint semântico: DELETE /api/session-management/sessions/{id}
     */
    @Operation(
        summary = "Encerrar sessão", 
        description = "Encerra uma sessão específica, invalidando o token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Sessão encerrada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Sessão não encontrada"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> terminateSession(
            @Parameter(description = "ID da sessão") @PathVariable Long id) {
        sessionService.terminateSession(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * CASO DE USO: Administrador encerra todas as sessões de um usuário
     * Endpoint semântico: DELETE /api/session-management/users/{userId}/sessions
     */
    @Operation(
        summary = "Encerrar todas as sessões do usuário", 
        description = "Encerra todas as sessões ativas de um usuário específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Todas as sessões encerradas"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @DeleteMapping("/users/{userId}/sessions")
    public ResponseEntity<Void> terminateAllUserSessions(
            @Parameter(description = "ID do usuário") @PathVariable Long userId) {
        sessionService.invalidateAllUserSessions(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * CASO DE USO: Administrador encerra sessões expiradas
     * Endpoint semântico: POST /api/session-management/sessions/cleanup
     */
    @Operation(
        summary = "Limpar sessões expiradas", 
        description = "Remove todas as sessões expiradas do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Limpeza realizada com sucesso"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @PostMapping("/sessions/cleanup")
    public ResponseEntity<Object> cleanupExpiredSessions() {
        Long cleanedCount = sessionService.cleanupExpiredSessions();
        return ResponseEntity.ok(new Object() {
            public final String message = "Limpeza de sessões concluída";
            public final Long cleanedSessions = cleanedCount;
        });
    }

    /**
     * CASO DE USO: Administrador obtém estatísticas de sessões
     * Endpoint semântico: GET /api/session-management/sessions/stats
     */
    @Operation(
        summary = "Obter estatísticas de sessões", 
        description = "Retorna estatísticas gerais das sessões do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estatísticas obtidas"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/sessions/stats")
    public ResponseEntity<Object> getSessionStats() {
        return ResponseEntity.ok(new Object() {
            public final Long totalSessions = sessionService.countSessions();
            public final Long activeSessions = sessionService.countActiveSessions();
            public final Long expiredSessions = sessionService.countExpiredSessions();
            public final Long sessionsToday = sessionService.countSessionsToday();
        });
    }

    /**
     * CASO DE USO: Administrador busca sessões por critério
     * Endpoint semântico: GET /api/session-management/sessions/search
     */
    @Operation(
        summary = "Buscar sessões", 
        description = "Busca sessões por IP, user agent ou outros critérios"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Busca realizada",
                content = @Content(schema = @Schema(implementation = SessionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Critério de busca inválido"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/sessions/search")
    public ResponseEntity<List<SessionResponse>> searchSessions(
            @Parameter(description = "IP address") @RequestParam(required = false) String ipAddress,
            @Parameter(description = "User agent") @RequestParam(required = false) String userAgent,
            @Parameter(description = "User ID") @RequestParam(required = false) Long userId) {
        List<SessionResponse> sessions = sessionService.searchSessions(ipAddress, userAgent, userId).stream()
                .map(SessionMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    /**
     * CASO DE USO: Sistema verifica saúde da gestão de sessões
     * Endpoint semântico: GET /api/session-management/health
     */
    @Operation(
        summary = "Verificar saúde da gestão de sessões", 
        description = "Retorna status do sistema de gestão de sessões"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sistema funcionando normalmente")
    })
    @GetMapping("/health")
    public ResponseEntity<Object> getSessionManagementHealth() {
        return ResponseEntity.ok(new Object() {
            public final String status = "healthy";
            public final String service = "session-management";
            public final Long activeSessions = sessionService.countActiveSessions();
            public final Long expiredSessions = sessionService.countExpiredSessions();
        });
    }
}
