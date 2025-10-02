package com.seccreto.service.auth.config;

import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;
import com.seccreto.service.auth.repository.permissions.PermissionRepository;
import com.seccreto.service.auth.repository.roles.RoleRepository;
import com.seccreto.service.auth.repository.roles_permissions.RolesPermissionsRepository;
import com.seccreto.service.auth.repository.tenants.TenantRepository;
import com.seccreto.service.auth.repository.users.UserRepository;
import com.seccreto.service.auth.repository.users_tenants_roles.UsersTenantsRolesRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Inicializador de dados para popular o banco com roles e permissions básicas.
 * 
 * Executa apenas nos profiles de desenvolvimento e teste.
 */
@Component
@Profile({"dev", "test", "postgres"})
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolesPermissionsRepository rolesPermissionsRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final UsersTenantsRolesRepository usersTenantsRolesRepository;

    public DataInitializer(RoleRepository roleRepository,
                          PermissionRepository permissionRepository,
                          RolesPermissionsRepository rolesPermissionsRepository,
                          TenantRepository tenantRepository,
                          UserRepository userRepository,
                          UsersTenantsRolesRepository usersTenantsRolesRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolesPermissionsRepository = rolesPermissionsRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.usersTenantsRolesRepository = usersTenantsRolesRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("🚀 Inicializando dados básicos do sistema...");

        try {
            // 1. Criar tenant padrão
            Tenant defaultTenant = createDefaultTenant();
            
            // 2. Criar permissions
            List<Permission> permissions = createPermissions();
            
            // 3. Criar roles
            List<Role> roles = createRoles();
            
            // 4. Associar permissions aos roles
            associatePermissionsToRoles(roles, permissions);
            
            // 5. Associar usuários existentes ao tenant e roles
            associateExistingUsersToRoles(defaultTenant, roles);
            
            logger.info("✅ Dados básicos inicializados com sucesso!");
            
        } catch (Exception e) {
            logger.error("❌ Erro ao inicializar dados básicos: {}", e.getMessage(), e);
        }
    }

    private Tenant createDefaultTenant() {
        if (tenantRepository.count() == 0) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode config = mapper.createObjectNode()
                    .put("type", "default")
                    .put("description", "Default organization tenant");
                
                Tenant tenant = Tenant.createNew("Default Organization", config);
                tenant = tenantRepository.save(tenant);
                logger.info("📋 Tenant padrão criado: {}", tenant.getName());
                return tenant;
            } catch (Exception e) {
                logger.error("Erro ao criar tenant padrão", e);
                throw new RuntimeException("Falha ao criar tenant padrão", e);
            }
        }
        return tenantRepository.findAll().get(0);
    }

    private List<Permission> createPermissions() {
        List<String> permissionActions = List.of(
            // User permissions
            "create:users", "read:users", "update:users", "delete:users",
            "read:profile", "update:profile",
            
            // Role permissions
            "create:roles", "read:roles", "update:roles", "delete:roles",
            
            // Permission permissions
            "create:permissions", "read:permissions", "update:permissions", "delete:permissions",
            
            // Tenant permissions
            "create:tenants", "read:tenants", "update:tenants", "delete:tenants",
            
            // Session permissions
            "read:sessions", "delete:sessions", "manage:sessions",
            
            // System permissions
            "read:metrics", "read:health", "manage:system"
        );

        for (String actionResource : permissionActions) {
            String[] parts = actionResource.split(":");
            String action = parts[0];
            String resource = parts[1];
            
            if (!permissionRepository.existsByActionAndResource(action, resource)) {
                Permission permission = Permission.createNew(action, resource);
                permissionRepository.save(permission);
                logger.info("🔑 Permission criada: {}:{}", action, resource);
            }
        }

        return permissionRepository.findAll();
    }

    private List<Role> createRoles() {
        // Super Admin - acesso total
        if (!roleRepository.existsByName("SUPER_ADMIN")) {
            Role superAdmin = Role.createNew("SUPER_ADMIN", "Super Administrator with full system access");
            roleRepository.save(superAdmin);
            logger.info("👑 Role criada: SUPER_ADMIN");
        }

        // Admin - administração geral
        if (!roleRepository.existsByName("ADMIN")) {
            Role admin = Role.createNew("ADMIN", "Administrator with management permissions");
            roleRepository.save(admin);
            logger.info("🛡️ Role criada: ADMIN");
        }

        // Manager - gerenciamento de usuários
        if (!roleRepository.existsByName("MANAGER")) {
            Role manager = Role.createNew("MANAGER", "Manager with user management permissions");
            roleRepository.save(manager);
            logger.info("👨‍💼 Role criada: MANAGER");
        }

        // User - usuário padrão
        if (!roleRepository.existsByName("USER")) {
            Role user = Role.createNew("USER", "Standard user with basic permissions");
            roleRepository.save(user);
            logger.info("👤 Role criada: USER");
        }

        return roleRepository.findAll();
    }

    private void associatePermissionsToRoles(List<Role> roles, List<Permission> permissions) {
        Role superAdmin = roles.stream().filter(r -> "SUPER_ADMIN".equals(r.getName())).findFirst().orElse(null);
        Role admin = roles.stream().filter(r -> "ADMIN".equals(r.getName())).findFirst().orElse(null);
        Role manager = roles.stream().filter(r -> "MANAGER".equals(r.getName())).findFirst().orElse(null);
        Role user = roles.stream().filter(r -> "USER".equals(r.getName())).findFirst().orElse(null);

        // SUPER_ADMIN - todas as permissions
        if (superAdmin != null) {
            for (Permission permission : permissions) {
                if (!rolesPermissionsRepository.existsByRoleIdAndPermissionId(superAdmin.getId(), permission.getId())) {
                    RolesPermissions rp = RolesPermissions.createNew(superAdmin.getId(), permission.getId());
                    rolesPermissionsRepository.save(rp);
                }
            }
            logger.info("🔗 SUPER_ADMIN associado a todas as permissions");
        }

        // ADMIN - permissions administrativas
        if (admin != null) {
            List<String> adminPermissions = List.of(
                "create:users", "read:users", "update:users", "delete:users",
                "read:roles", "read:permissions", "read:tenants",
                "read:sessions", "delete:sessions", "manage:sessions",
                "read:metrics", "read:health"
            );
            associatePermissionsToRole(admin, permissions, adminPermissions);
            logger.info("🔗 ADMIN associado às permissions administrativas");
        }

        // MANAGER - permissions de gerenciamento
        if (manager != null) {
            List<String> managerPermissions = List.of(
                "create:users", "read:users", "update:users",
                "read:roles", "read:sessions", "read:metrics"
            );
            associatePermissionsToRole(manager, permissions, managerPermissions);
            logger.info("🔗 MANAGER associado às permissions de gerenciamento");
        }

        // USER - permissions básicas
        if (user != null) {
            List<String> userPermissions = List.of(
                "read:profile", "update:profile", "read:health"
            );
            associatePermissionsToRole(user, permissions, userPermissions);
            logger.info("🔗 USER associado às permissions básicas");
        }
    }

    private void associatePermissionsToRole(Role role, List<Permission> permissions, List<String> permissionActions) {
        for (String action : permissionActions) {
            Permission permission = permissions.stream()
                    .filter(p -> action.equals(p.getAction()))
                    .findFirst()
                    .orElse(null);
            
            if (permission != null && !rolesPermissionsRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
                RolesPermissions rp = RolesPermissions.createNew(role.getId(), permission.getId());
                rolesPermissionsRepository.save(rp);
            }
        }
    }

    private void associateExistingUsersToRoles(Tenant defaultTenant, List<Role> roles) {
        Role userRole = roles.stream().filter(r -> "USER".equals(r.getName())).findFirst().orElse(null);
        
        if (userRole != null) {
            userRepository.findAll().forEach(user -> {
                // Verificar se usuário já tem associação
                boolean hasAssociation = usersTenantsRolesRepository.existsByUserIdAndTenantId(user.getId(), defaultTenant.getId());
                
                if (!hasAssociation) {
                    UsersTenantsRoles utr = UsersTenantsRoles.createNew(user.getId(), defaultTenant.getId(), userRole.getId());
                    usersTenantsRolesRepository.save(utr);
                    logger.info("🔗 Usuário {} associado ao role USER no tenant padrão", user.getEmail());
                }
            });
        }
    }
}
