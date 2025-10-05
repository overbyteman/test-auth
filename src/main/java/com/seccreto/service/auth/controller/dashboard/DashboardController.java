package com.seccreto.service.auth.controller.dashboard;

import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.tenants.TenantService;
import com.seccreto.service.auth.service.sessions.SessionService;
import com.seccreto.service.auth.service.roles.RoleService;
import com.seccreto.service.auth.service.permissions.PermissionService;
import com.seccreto.service.auth.service.policies.PolicyService;
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


/**
 * Controller consolidado para dashboard e relatórios.
 * OTIMIZADO: 10 endpoints → 4 endpoints
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard e Relatórios", description = "Endpoints consolidados para métricas e relatórios do sistema")
public class DashboardController {

    private final UserService userService;
    private final TenantService tenantService;
    private final SessionService sessionService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final PolicyService policyService;
    private final UsersTenantsRolesService usersTenantsRolesService;

    public DashboardController(UserService userService, 
                              TenantService tenantService,
                              SessionService sessionService,
                              RoleService roleService,
                              PermissionService permissionService,
                              PolicyService policyService,
                              UsersTenantsRolesService usersTenantsRolesService) {
        this.userService = userService;
        this.tenantService = tenantService;
        this.sessionService = sessionService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.policyService = policyService;
        this.usersTenantsRolesService = usersTenantsRolesService;
    }

}