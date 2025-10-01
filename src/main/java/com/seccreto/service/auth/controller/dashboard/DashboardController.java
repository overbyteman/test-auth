package com.seccreto.service.auth.controller.dashboard;

import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.tenants.TenantService;
import com.seccreto.service.auth.service.sessions.SessionService;
import com.seccreto.service.auth.service.roles.RoleService;
import com.seccreto.service.auth.service.permissions.PermissionService;
import com.seccreto.service.auth.service.policies.PolicyService;
import com.seccreto.service.auth.service.roles_permissions.RolesPermissionsService;
import com.seccreto.service.auth.service.users_tenants_roles.UsersTenantsRolesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller semântico para dashboard e relatórios.
 * Endpoints baseados em casos de uso reais de análise e monitoramento.
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard e Relatórios", description = "Endpoints semânticos para métricas e relatórios do sistema")
public class DashboardController {

    private final UserService userService;
    private final TenantService tenantService;
    private final SessionService sessionService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final PolicyService policyService;
    private final RolesPermissionsService rolesPermissionsService;
    private final UsersTenantsRolesService usersTenantsRolesService;

    public DashboardController(UserService userService, 
                              TenantService tenantService,
                              SessionService sessionService,
                              RoleService roleService,
                              PermissionService permissionService,
                              PolicyService policyService,
                              RolesPermissionsService rolesPermissionsService,
                              UsersTenantsRolesService usersTenantsRolesService) {
        this.userService = userService;
        this.tenantService = tenantService;
        this.sessionService = sessionService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.policyService = policyService;
        this.rolesPermissionsService = rolesPermissionsService;
        this.usersTenantsRolesService = usersTenantsRolesService;
    }

    /**
     * CASO DE USO: Administrador obtém visão geral do sistema
     * Endpoint semântico: GET /api/dashboard/overview
     */
    @Operation(
        summary = "Obter visão geral do sistema", 
        description = "Retorna métricas gerais e estatísticas do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Visão geral obtida"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/overview")
    public ResponseEntity<Object> getSystemOverview() {
        return ResponseEntity.ok(new Object() {
            public final String timestamp = LocalDateTime.now().toString();
            public final Long totalUsers = userService.countUsers();
            public final Long activeUsers = userService.countActiveUsers();
            public final Long totalTenants = tenantService.countTenants();
            public final Long activeTenants = tenantService.countActiveTenants();
            public final Long activeSessions = sessionService.countActiveSessions();
            public final Long totalRoles = roleService.countRoles();
            public final Long totalPermissions = permissionService.countPermissions();
            public final Long totalPolicies = policyService.countPolicies();
        });
    }

    /**
     * CASO DE USO: Administrador obtém métricas de usuários
     * Endpoint semântico: GET /api/dashboard/users/metrics
     */
    @Operation(
        summary = "Obter métricas de usuários", 
        description = "Retorna métricas detalhadas sobre usuários do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Métricas obtidas"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/users/metrics")
    public ResponseEntity<Object> getUserMetrics() {
        return ResponseEntity.ok(new Object() {
            public final Long totalUsers = userService.countUsers();
            public final Long activeUsers = userService.countActiveUsers();
            public final Long suspendedUsers = userService.countSuspendedUsers();
            public final Long usersToday = userService.countUsersCreatedToday();
            public final Long usersThisWeek = userService.countUsersCreatedThisWeek();
            public final Long usersThisMonth = userService.countUsersCreatedThisMonth();
        });
    }

    /**
     * CASO DE USO: Administrador obtém métricas de sessões
     * Endpoint semântico: GET /api/dashboard/sessions/metrics
     */
    @Operation(
        summary = "Obter métricas de sessões", 
        description = "Retorna métricas detalhadas sobre sessões do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Métricas obtidas"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/sessions/metrics")
    public ResponseEntity<Object> getSessionMetrics() {
        return ResponseEntity.ok(new Object() {
            public final Long totalSessions = sessionService.countSessions();
            public final Long activeSessions = sessionService.countActiveSessions();
            public final Long expiredSessions = sessionService.countExpiredSessions();
            public final Long sessionsToday = sessionService.countSessionsToday();
            public final Long sessionsThisWeek = sessionService.countSessionsThisWeek();
            public final Long sessionsThisMonth = sessionService.countSessionsThisMonth();
        });
    }

    /**
     * CASO DE USO: Administrador obtém métricas de tenants
     * Endpoint semântico: GET /api/dashboard/tenants/metrics
     */
    @Operation(
        summary = "Obter métricas de tenants", 
        description = "Retorna métricas detalhadas sobre tenants do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Métricas obtidas"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/tenants/metrics")
    public ResponseEntity<Object> getTenantMetrics() {
        return ResponseEntity.ok(new Object() {
            public final Long totalTenants = tenantService.countTenants();
            public final Long activeTenants = tenantService.countActiveTenants();
            public final Long inactiveTenants = tenantService.countInactiveTenants();
            public final Long tenantsToday = tenantService.countTenantsCreatedToday();
            public final Long tenantsThisWeek = tenantService.countTenantsCreatedThisWeek();
            public final Long tenantsThisMonth = tenantService.countTenantsCreatedThisMonth();
        });
    }

    /**
     * CASO DE USO: Administrador obtém métricas de segurança
     * Endpoint semântico: GET /api/dashboard/security/metrics
     */
    @Operation(
        summary = "Obter métricas de segurança", 
        description = "Retorna métricas relacionadas à segurança do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Métricas obtidas"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/security/metrics")
    public ResponseEntity<Object> getSecurityMetrics() {
        return ResponseEntity.ok(new Object() {
            public final Long totalRoles = roleService.countRoles();
            public final Long totalPermissions = permissionService.countPermissions();
            public final Long totalPolicies = policyService.countPolicies();
            public final Long activePolicies = policyService.countActivePolicies();
            public final Long rolePermissionAssociations = rolesPermissionsService.countAssociations();
            public final Long userRoleAssociations = usersTenantsRolesService.countAssociations();
        });
    }

    /**
     * CASO DE USO: Administrador obtém relatório de atividade
     * Endpoint semântico: GET /api/dashboard/activity/report
     */
    @Operation(
        summary = "Obter relatório de atividade", 
        description = "Retorna relatório de atividade do sistema em um período específico"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Relatório obtido"),
        @ApiResponse(responseCode = "400", description = "Período inválido"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/activity/report")
    public ResponseEntity<Object> getActivityReport(
            @Parameter(description = "Data inicial (YYYY-MM-DD)") @RequestParam(required = false) String startDate,
            @Parameter(description = "Data final (YYYY-MM-DD)") @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(new Object() {
            public final String period = startDate + " to " + endDate;
            public final Long newUsers = userService.countUsersInPeriod(startDate, endDate);
            public final Long newSessions = sessionService.countSessionsInPeriod(startDate, endDate);
            public final Long newTenants = tenantService.countTenantsInPeriod(startDate, endDate);
            public final Long activeUsers = userService.countActiveUsersInPeriod(startDate, endDate);
            public final Long activeSessions = sessionService.countActiveSessionsInPeriod(startDate, endDate);
        });
    }

    /**
     * CASO DE USO: Administrador obtém top usuários ativos
     * Endpoint semântico: GET /api/dashboard/users/top-active
     */
    @Operation(
        summary = "Obter top usuários ativos", 
        description = "Retorna lista dos usuários mais ativos do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista obtida"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/users/top-active")
    public ResponseEntity<List<Object>> getTopActiveUsers(
            @Parameter(description = "Número de usuários a retornar") @RequestParam(defaultValue = "10") int limit) {
        List<Object> topUsers = userService.findTopActiveUsers(limit).stream()
                .map(user -> new Object() {
                    public final Long userId = user.getId();
                    public final String name = user.getName();
                    public final String email = user.getEmail();
                    public final Long sessionCount = sessionService.countSessionsByUser(user.getId());
                    public final String lastLogin = sessionService.findLastLoginByUser(user.getId());
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(topUsers);
    }

    /**
     * CASO DE USO: Administrador obtém distribuição de roles
     * Endpoint semântico: GET /api/dashboard/roles/distribution
     */
    @Operation(
        summary = "Obter distribuição de roles", 
        description = "Retorna distribuição de usuários por role"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Distribuição obtida"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/roles/distribution")
    public ResponseEntity<List<Object>> getRoleDistribution() {
        List<Object> distribution = roleService.getRoleDistribution().entrySet().stream()
                .map(entry -> new Object() {
                    public final String roleName = entry.getKey();
                    public final Long userCount = entry.getValue();
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(distribution);
    }

    /**
     * CASO DE USO: Administrador obtém saúde geral do sistema
     * Endpoint semântico: GET /api/dashboard/system/health
     */
    @Operation(
        summary = "Obter saúde do sistema", 
        description = "Retorna status de saúde de todos os componentes do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status obtido")
    })
    @GetMapping("/system/health")
    public ResponseEntity<Object> getSystemHealth() {
        return ResponseEntity.ok(new Object() {
            public final String status = "healthy";
            public final String timestamp = LocalDateTime.now().toString();
            public final Object users = new Object() {
                public final String status = "healthy";
                public final Long total = userService.countUsers();
                public final Long active = userService.countActiveUsers();
            };
            public final Object tenants = new Object() {
                public final String status = "healthy";
                public final Long total = tenantService.countTenants();
                public final Long active = tenantService.countActiveTenants();
            };
            public final Object sessions = new Object() {
                public final String status = "healthy";
                public final Long total = sessionService.countSessions();
                public final Long active = sessionService.countActiveSessions();
            };
            public final Object security = new Object() {
                public final String status = "healthy";
                public final Long roles = roleService.countRoles();
                public final Long permissions = permissionService.countPermissions();
                public final Long policies = policyService.countPolicies();
            };
        });
    }

    /**
     * CASO DE USO: Sistema exporta relatório completo
     * Endpoint semântico: GET /api/dashboard/export/report
     */
    @Operation(
        summary = "Exportar relatório completo", 
        description = "Exporta relatório completo do sistema em formato JSON"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Relatório exportado"),
        @ApiResponse(responseCode = "403", description = "Permissões insuficientes")
    })
    @GetMapping("/export/report")
    public ResponseEntity<Object> exportFullReport() {
        return ResponseEntity.ok(new Object() {
            public final String exportDate = LocalDateTime.now().toString();
            public final String version = "1.0.0";
            public final Object summary = new Object() {
                public final Long totalUsers = userService.countUsers();
                public final Long totalTenants = tenantService.countTenants();
                public final Long totalSessions = sessionService.countSessions();
                public final Long totalRoles = roleService.countRoles();
                public final Long totalPermissions = permissionService.countPermissions();
                public final Long totalPolicies = policyService.countPolicies();
            };
            public final Object metrics = new Object() {
                public final Long activeUsers = userService.countActiveUsers();
                public final Long activeTenants = tenantService.countActiveTenants();
                public final Long activeSessions = sessionService.countActiveSessions();
                public final Long activePolicies = policyService.countActivePolicies();
            };
        });
    }
}
