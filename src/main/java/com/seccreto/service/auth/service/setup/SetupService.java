package com.seccreto.service.auth.service.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.api.dto.tenants.TenantRequest;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.repository.landlords.LandlordRepository;
import com.seccreto.service.auth.repository.permissions.PermissionRepository;
import com.seccreto.service.auth.repository.policies.PolicyRepository;
import com.seccreto.service.auth.repository.roles.RoleRepository;
import com.seccreto.service.auth.repository.tenants.TenantRepository;
import com.seccreto.service.auth.service.tenants.TenantService;
import com.seccreto.service.auth.api.mapper.tenants.TenantMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para setup e configuração de novas redes de academias.
 * 
 * Responsabilidades:
 * - Criar landlords (matrizes) com roles padrões
 * - Configurar permissões e políticas para academias de luta
 * - Adicionar tenants (filiais) às redes
 * - Verificar status de configuração
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SetupService {

    private static final Logger log = LoggerFactory.getLogger(SetupService.class);

    private final LandlordRepository landlordRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PolicyRepository policyRepository;
    private final TenantRepository tenantRepository;
    private final TenantService tenantService;
    private final ObjectMapper objectMapper;
    private static final String PRIMARY_TENANT_SUFFIX = " HQ";
    private static final String PRIMARY_TENANT_FLAG = "is_primary_tenant";

    /**
     * Adiciona um tenant (filial) a uma rede existente
     */
    @Transactional
    public TenantResponse addTenantToNetwork(UUID landlordId, TenantRequest request) {
        log.info("🏢 Adicionando filial '{}' à rede {}", request.getName(), landlordId);
        
        landlordRepository.findById(landlordId)
            .orElseThrow(() -> new IllegalArgumentException("Landlord não encontrado: " + landlordId));

        // Criar tenant
        TenantRequest tenantRequest = new TenantRequest();
        tenantRequest.setName(request.getName());
        tenantRequest.setConfig(request.getConfig());
        
        Tenant tenant = tenantService.createTenant(
            tenantRequest.getName(), 
            tenantRequest.getConfig(), 
            landlordId
        );

        log.info("✅ Filial '{}' adicionada à rede com ID: {}", request.getName(), tenant.getId());
        return TenantMapper.toResponse(tenant);
    }

    /**
     * Configura roles padrões para um landlord existente
     */
    @Transactional
    public int setupDefaultRolesForLandlord(UUID landlordId) {
        log.info("🥊 Configurando roles padrões para landlord: {}", landlordId);
        
        Landlord landlord = landlordRepository.findById(landlordId)
            .orElseThrow(() -> new IllegalArgumentException("Landlord não encontrado: " + landlordId));

        Tenant primaryTenant = ensurePrimaryTenant(landlord);

        SyncResult<Policy> policyResult = synchronizeDefaultPolicies(landlord, primaryTenant);
        SyncResult<Permission> permissionResult = synchronizeDefaultPermissions(landlord, policyResult.entitiesByKey());
        SyncResult<Role> roleResult = synchronizeDefaultRoles(landlord, permissionResult.entitiesByKey());

        log.info("✅ Policies asseguradas: {} criadas, {} atualizadas", policyResult.created(), policyResult.updated());
        log.info("✅ Permissions asseguradas: {} criadas, {} atualizadas", permissionResult.created(), permissionResult.updated());
        log.info("✅ Roles asseguradas: {} criadas, {} atualizadas", roleResult.created(), roleResult.updated());

        return roleResult.created();
    }

    // ===== MÉTODOS PRIVADOS =====

    private Tenant ensurePrimaryTenant(Landlord landlord) {
        List<Tenant> tenants = tenantService.findTenantsByLandlordId(landlord.getId());

        Optional<Tenant> primaryByFlag = tenants.stream()
                .filter(tenant -> tenant.getConfig() != null && tenant.getConfig().path(PRIMARY_TENANT_FLAG).asBoolean(false))
                .findFirst();
        if (primaryByFlag.isPresent()) {
            return primaryByFlag.get();
        }

        String defaultTenantName = landlord.getName() + PRIMARY_TENANT_SUFFIX;
        Optional<Tenant> primaryByName = tenants.stream()
                .filter(tenant -> tenant.getName() != null && tenant.getName().equalsIgnoreCase(defaultTenantName))
                .findFirst();
        if (primaryByName.isPresent()) {
            return primaryByName.get();
        }

        JsonNode config = buildDefaultTenantConfig(landlord);
        return tenantService.createTenant(defaultTenantName, config, landlord.getId());
    }

    private JsonNode buildDefaultTenantConfig(Landlord landlord) {
        String timezone = Optional.ofNullable(landlord.getConfig())
                .map(cfg -> cfg.path("timezone").asText(null))
                .filter(value -> value != null && !value.isBlank())
                .orElse("America/Sao_Paulo");

        String currency = Optional.ofNullable(landlord.getConfig())
                .map(cfg -> cfg.path("default_currency").asText(null))
                .filter(value -> value != null && !value.isBlank())
                .orElse("BRL");

        long policiesCount = policyRepository.countByLandlordId(landlord.getId());
        long permissionsCount = permissionRepository.countByLandlordId(landlord.getId());
        long rolesCount = roleRepository.countByLandlordId(landlord.getId());
        long tenantsCount = tenantRepository.countByLandlordId(landlord.getId());

        com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "headquarters");
        root.put("region", "primary");
        root.put("landlord_id", landlord.getId().toString());
        root.put("landlord_name", landlord.getName());
        root.put("timezone", timezone);
        root.put("default_currency", currency);
        root.put(PRIMARY_TENANT_FLAG, true);

        com.fasterxml.jackson.databind.node.ArrayNode features = root.putArray("features");
        features.add("member_management");
        features.add("class_scheduling");
        features.add("billing");
        features.add("reporting");

        com.fasterxml.jackson.databind.node.ObjectNode bootstrap = root.putObject("bootstrap_metrics");
        bootstrap.put("existing_policies", policiesCount);
        bootstrap.put("existing_permissions", permissionsCount);
        bootstrap.put("existing_roles", rolesCount);
        bootstrap.put("existing_tenants", tenantsCount);

        return root;
    }

    private SyncResult<Policy> synchronizeDefaultPolicies(Landlord landlord, Tenant tenant) {
        Map<String, PolicyDefinition> definitions = defaultPolicyDefinitions();
        Map<String, Policy> persisted = new LinkedHashMap<>();
        int created = 0;
        int updated = 0;

        for (Map.Entry<String, PolicyDefinition> entry : definitions.entrySet()) {
            String code = entry.getKey();
            PolicyDefinition definition = entry.getValue();

            Optional<Policy> existingOpt = policyRepository.findByTenantIdAndCode(tenant.getId(), code);
            if (existingOpt.isPresent()) {
                Policy existing = existingOpt.get();
                boolean dirty = false;

                if (!Objects.equals(existing.getName(), definition.name())) {
                    existing.setName(definition.name());
                    dirty = true;
                }
                if (!Objects.equals(existing.getDescription(), definition.description())) {
                    existing.setDescription(definition.description());
                    dirty = true;
                }
                if (existing.getEffect() != definition.effect()) {
                    existing.setEffect(definition.effect());
                    dirty = true;
                }
                if (!Objects.equals(existing.getActions(), definition.actions())) {
                    existing.setActions(List.copyOf(definition.actions()));
                    dirty = true;
                }
                if (!Objects.equals(existing.getResources(), definition.resources())) {
                    existing.setResources(List.copyOf(definition.resources()));
                    dirty = true;
                }
                if (!Objects.equals(existing.getConditions(), definition.conditions())) {
                    existing.setConditions(definition.conditions().deepCopy());
                    dirty = true;
                }

                if (dirty) {
                    existing = policyRepository.save(existing);
                    updated++;
                }
                persisted.put(code, existing);
            } else {
                Policy policy = Policy.createNew(
                        tenant,
                        definition.code(),
                        definition.name(),
                        definition.description(),
                        definition.effect(),
                        List.copyOf(definition.actions()),
                        List.copyOf(definition.resources()),
                        definition.conditions().deepCopy()
                );
                Policy saved = policyRepository.save(policy);
                persisted.put(code, saved);
                created++;
            }
        }

        return new SyncResult<>(created, updated, persisted);
    }

    private Map<String, PolicyDefinition> defaultPolicyDefinitions() {
        Map<String, PolicyDefinition> definitions = new LinkedHashMap<>();

        definitions.put("admin_full_access", new PolicyDefinition(
                "admin_full_access",
                "Acesso Administrativo Completo",
                "Acesso total a todas as funcionalidades da academia",
                PolicyEffect.ALLOW,
                List.of("create", "read", "update", "delete", "manage"),
                List.of("users", "roles", "permissions", "policies", "members", "classes", "payments", "reports", "settings"),
                objectMapper.createObjectNode()
                        .put("time_restriction", "business_hours")
                        .put("ip_whitelist", true)
        ));

        definitions.put("instructor_access", new PolicyDefinition(
                "instructor_access",
                "Acesso de Instrutor",
                "Acesso para gerenciar alunos e aulas",
                PolicyEffect.ALLOW,
                List.of("read", "update", "create"),
                List.of("members", "classes", "attendance", "progress"),
                objectMapper.createObjectNode()
                        .put("role_restriction", "instructor")
                        .put("class_time_access", true)
        ));

        definitions.put("financial_access", new PolicyDefinition(
                "financial_access",
                "Acesso Financeiro",
                "Acesso a informações financeiras e pagamentos",
                PolicyEffect.ALLOW,
                List.of("read", "create", "update"),
                List.of("payments", "invoices", "financial_reports", "members"),
                objectMapper.createObjectNode()
                        .put("department", "financial")
                        .put("data_sensitivity", "high")
        ));

        definitions.put("reception_access", new PolicyDefinition(
                "reception_access",
                "Acesso de Recepção",
                "Acesso básico para atendimento ao público",
                PolicyEffect.ALLOW,
                List.of("create", "read", "update"),
                List.of("members", "classes", "schedule", "basic_reports"),
                objectMapper.createObjectNode()
                        .put("public_facing", true)
                        .put("requires_training", true)
        ));

        return definitions;
    }

    private SyncResult<Permission> synchronizeDefaultPermissions(Landlord landlord, Map<String, Policy> policiesByCode) {
        Map<String, PermissionDefinition> definitions = defaultPermissionDefinitions();
        Map<String, Permission> persisted = new LinkedHashMap<>();
        int created = 0;
        int updated = 0;

        for (Map.Entry<String, PermissionDefinition> entry : definitions.entrySet()) {
            String key = entry.getKey();
            PermissionDefinition definition = entry.getValue();
            Policy policy = policiesByCode.get(definition.policyCode());

            if (policy == null) {
                log.warn("Policy '{}' não encontrada ao configurar permissão '{}'", definition.policyCode(), key);
                continue;
            }

            Optional<Permission> existingOpt = permissionRepository.findByLandlordIdAndActionAndResource(
                    landlord.getId(), definition.action(), definition.resource());

            if (existingOpt.isPresent()) {
                Permission existing = existingOpt.get();
                boolean dirty = false;

                if (!Objects.equals(existing.getAction(), definition.action())) {
                    existing.setAction(definition.action());
                    dirty = true;
                }
                if (!Objects.equals(existing.getResource(), definition.resource())) {
                    existing.setResource(definition.resource());
                    dirty = true;
                }
                if (!Objects.equals(existing.getPolicy(), policy)) {
                    existing.setPolicy(policy);
                    dirty = true;
                }

                if (dirty) {
                    existing = permissionRepository.save(existing);
                    updated++;
                }
                persisted.put(key, existing);
            } else {
                Permission permission = Permission.createNew(definition.action(), definition.resource(), policy, landlord);
                Permission saved = permissionRepository.save(permission);
                persisted.put(key, saved);
                created++;
            }
        }

        return new SyncResult<>(created, updated, persisted);
    }

    private Map<String, PermissionDefinition> defaultPermissionDefinitions() {
        Map<String, PermissionDefinition> definitions = new LinkedHashMap<>();

        definitions.put("manage_users", new PermissionDefinition("manage_users", "manage", "users", "admin_full_access"));
        definitions.put("manage_roles", new PermissionDefinition("manage_roles", "manage", "roles", "admin_full_access"));
        definitions.put("manage_permissions", new PermissionDefinition("manage_permissions", "manage", "permissions", "admin_full_access"));
        definitions.put("manage_policies", new PermissionDefinition("manage_policies", "manage", "policies", "admin_full_access"));
        definitions.put("manage_settings", new PermissionDefinition("manage_settings", "manage", "settings", "admin_full_access"));

        definitions.put("create_members", new PermissionDefinition("create_members", "create", "members", "reception_access"));
        definitions.put("read_members", new PermissionDefinition("read_members", "read", "members", "reception_access"));
        definitions.put("update_members", new PermissionDefinition("update_members", "update", "members", "instructor_access"));
        definitions.put("delete_members", new PermissionDefinition("delete_members", "delete", "members", "admin_full_access"));

        definitions.put("create_classes", new PermissionDefinition("create_classes", "create", "classes", "instructor_access"));
        definitions.put("read_classes", new PermissionDefinition("read_classes", "read", "classes", "reception_access"));
        definitions.put("update_classes", new PermissionDefinition("update_classes", "update", "classes", "instructor_access"));
        definitions.put("delete_classes", new PermissionDefinition("delete_classes", "delete", "classes", "admin_full_access"));

        definitions.put("read_payments", new PermissionDefinition("read_payments", "read", "payments", "financial_access"));
        definitions.put("create_payments", new PermissionDefinition("create_payments", "create", "payments", "financial_access"));
        definitions.put("update_payments", new PermissionDefinition("update_payments", "update", "payments", "financial_access"));
        definitions.put("read_financial_reports", new PermissionDefinition("read_financial_reports", "read", "financial_reports", "financial_access"));

        definitions.put("read_reports", new PermissionDefinition("read_reports", "read", "reports", "admin_full_access"));
        definitions.put("read_basic_reports", new PermissionDefinition("read_basic_reports", "read", "basic_reports", "reception_access"));

        definitions.put("manage_equipment", new PermissionDefinition("manage_equipment", "manage", "equipment", "admin_full_access"));
        definitions.put("read_equipment", new PermissionDefinition("read_equipment", "read", "equipment", "reception_access"));

        definitions.put("manage_competitions", new PermissionDefinition("manage_competitions", "manage", "competitions", "instructor_access"));
        definitions.put("read_competitions", new PermissionDefinition("read_competitions", "read", "competitions", "reception_access"));

        return definitions;
    }

    private SyncResult<Role> synchronizeDefaultRoles(Landlord landlord, Map<String, Permission> permissionsByKey) {
        List<RoleDefinition> definitions = defaultRoleDefinitions();
        Map<String, Role> persisted = new LinkedHashMap<>();
        int created = 0;
        int updated = 0;

        for (RoleDefinition definition : definitions) {
            Optional<Role> existingOpt = roleRepository.findByCodeAndLandlordId(definition.code(), landlord.getId());
            if (existingOpt.isPresent()) {
                Role existing = existingOpt.get();
                boolean dirty = false;

                if (!Objects.equals(existing.getName(), definition.name())) {
                    existing.setName(definition.name());
                    dirty = true;
                }
                if (!Objects.equals(existing.getDescription(), definition.description())) {
                    existing.setDescription(definition.description());
                    dirty = true;
                }
                if (reconcileRolePermissions(existing, permissionsByKey, definition.permissionKeys())) {
                    dirty = true;
                }

                if (dirty) {
                    existing = roleRepository.save(existing);
                    updated++;
                }

                persisted.put(definition.code(), existing);
            } else {
                Role role = buildRoleFromDefinition(definition, landlord, permissionsByKey);
                Role saved = roleRepository.save(role);
                persisted.put(definition.code(), saved);
                created++;
            }
        }

        return new SyncResult<>(created, updated, persisted);
    }

    private List<RoleDefinition> defaultRoleDefinitions() {
        return List.of(
                new RoleDefinition(
                        "owner",
                        "PROPRIETÁRIO",
                        "Proprietário da academia com acesso total ao sistema",
                        List.of(
                                "manage_users", "manage_roles", "manage_permissions", "manage_policies", "manage_settings",
                                "create_members", "read_members", "update_members", "delete_members",
                                "create_classes", "read_classes", "update_classes", "delete_classes",
                                "read_payments", "create_payments", "update_payments", "read_financial_reports",
                                "read_reports", "manage_equipment", "manage_competitions"
                        )
                ),
                new RoleDefinition(
                        "general_manager",
                        "GERENTE GERAL",
                        "Gerente responsável pela operação geral da academia",
                        List.of(
                                "read_members", "update_members", "create_members",
                                "create_classes", "read_classes", "update_classes", "delete_classes",
                                "read_payments", "create_payments", "update_payments", "read_financial_reports",
                                "read_reports", "read_equipment", "manage_competitions"
                        )
                ),
                new RoleDefinition(
                        "financial_manager",
                        "GERENTE FINANCEIRO",
                        "Responsável pela gestão financeira da academia",
                        List.of(
                                "read_members", "update_members",
                                "read_payments", "create_payments", "update_payments", "read_financial_reports",
                                "read_reports", "read_equipment"
                        )
                ),
                new RoleDefinition(
                        "head_instructor",
                        "INSTRUTOR CHEFE",
                        "Instrutor principal responsável por outros instrutores e aulas",
                        List.of(
                                "read_members", "update_members", "create_members",
                                "create_classes", "read_classes", "update_classes", "delete_classes",
                                "read_equipment", "manage_competitions", "read_competitions"
                        )
                ),
                new RoleDefinition(
                        "instructor",
                        "INSTRUTOR",
                        "Instrutor responsável por ministrar aulas e acompanhar alunos",
                        List.of(
                                "read_members", "update_members",
                                "create_classes", "read_classes", "update_classes",
                                "read_equipment", "read_competitions"
                        )
                ),
                new RoleDefinition(
                        "receptionist",
                        "RECEPCIONISTA",
                        "Responsável pelo atendimento ao público e cadastro de membros",
                        List.of(
                                "create_members", "read_members", "update_members",
                                "read_classes", "read_basic_reports", "read_equipment"
                        )
                ),
                new RoleDefinition(
                        "admin_assistant",
                        "ASSISTENTE ADMINISTRATIVO",
                        "Assistente para tarefas administrativas gerais",
                        List.of(
                                "read_members", "update_members",
                                "read_classes", "update_classes",
                                "read_payments", "read_basic_reports", "read_equipment"
                        )
                ),
                new RoleDefinition(
                        "equipment_technician",
                        "TÉCNICO DE EQUIPAMENTOS",
                        "Responsável pela manutenção e controle de equipamentos",
                        List.of(
                                "read_equipment", "manage_equipment", "read_members"
                        )
                ),
                new RoleDefinition(
                        "security",
                        "SEGURANÇA",
                        "Responsável pela segurança da academia",
                        List.of(
                                "read_members", "read_classes", "read_equipment"
                        )
                ),
                new RoleDefinition(
                        "vip_member",
                        "MEMBRO VIP",
                        "Membro com privilégios especiais na academia",
                        List.of(
                                "read_classes", "read_competitions"
                        )
                ),
                new RoleDefinition(
                        "regular_member",
                        "MEMBRO REGULAR",
                        "Membro comum da academia",
                        List.of(
                                "read_classes", "read_competitions"
                        )
                )
        );
    }

    private Role buildRoleFromDefinition(RoleDefinition definition, Landlord landlord, Map<String, Permission> permissionsByKey) {
        Role role = Role.createNew(definition.code(), definition.name(), definition.description(), landlord);
        addPermissionsToRole(role, permissionsByKey, definition.permissionKeys());
        return role;
    }

    private boolean reconcileRolePermissions(Role role, Map<String, Permission> permissionsByKey, List<String> desiredKeys) {
        Set<Permission> desired = desiredKeys.stream()
                .map(permissionsByKey::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Permission> current = new LinkedHashSet<>(role.getPermissions());
        boolean changed = false;

        for (Permission permission : current) {
            if (!desired.contains(permission)) {
                role.removePermission(permission);
                changed = true;
            }
        }

        for (Permission permission : desired) {
            if (!current.contains(permission)) {
                role.addPermission(permission);
                changed = true;
            }
        }

        return changed;
    }

    private void addPermissionsToRole(Role role, Map<String, Permission> permissionsByKey, List<String> permissionKeys) {
        for (String key : permissionKeys) {
            Permission permission = permissionsByKey.get(key);
            if (permission != null) {
                role.addPermission(permission);
            } else {
                log.warn("Permissão '{}' não encontrada ao configurar role '{}'", key, role.getCode());
            }
        }
    }

    private record PolicyDefinition(String code,
                                    String name,
                                    String description,
                                    PolicyEffect effect,
                                    List<String> actions,
                                    List<String> resources,
                                    JsonNode conditions) {
    }

    private record PermissionDefinition(String key,
                                        String action,
                                        String resource,
                                        String policyCode) {
    }

    private record RoleDefinition(String code,
                                  String name,
                                  String description,
                                  List<String> permissionKeys) {
    }

    private record SyncResult<T>(int created, int updated, Map<String, T> entities) {
        SyncResult {
            entities = Collections.unmodifiableMap(new LinkedHashMap<>(entities));
        }

        Map<String, T> entitiesByKey() {
            return entities;
        }
    }

}
