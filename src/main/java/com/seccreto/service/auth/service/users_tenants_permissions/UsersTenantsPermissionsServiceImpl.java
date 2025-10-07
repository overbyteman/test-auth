package com.seccreto.service.auth.service.users_tenants_permissions;

import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.model.users_tenants_permissions.UsersTenantsPermissions;
import com.seccreto.service.auth.repository.permissions.PermissionRepository;
import com.seccreto.service.auth.repository.tenants.TenantRepository;
import com.seccreto.service.auth.repository.users.UserRepository;
import com.seccreto.service.auth.repository.users_tenants_permissions.UsersTenantsPermissionsRepository;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementação da camada de serviço contendo regras de negócio para users_tenants_permissions.
 * Espelha a estrutura e padrões utilizados em UsersTenantsRolesServiceImpl, garantindo consistência.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class UsersTenantsPermissionsServiceImpl implements UsersTenantsPermissionsService {

    private final UsersTenantsPermissionsRepository usersTenantsPermissionsRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;

    public UsersTenantsPermissionsServiceImpl(UsersTenantsPermissionsRepository usersTenantsPermissionsRepository,
                                              UserRepository userRepository,
                                              TenantRepository tenantRepository,
                                              PermissionRepository permissionRepository) {
        this.usersTenantsPermissionsRepository = usersTenantsPermissionsRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_permissions.create", description = "Tempo para criar permissão direta em tenant")
    public UsersTenantsPermissions createUserTenantPermission(UUID userId, UUID tenantId, UUID permissionId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validatePermissionId(permissionId);

        User user = requireUser(userId);
        Tenant tenant = requireTenant(tenantId);
        Permission permission = requirePermission(permissionId);

        ensureSameLandlord(tenant, permission);

        return usersTenantsPermissionsRepository
                .findByUserIdAndTenantIdAndPermissionId(userId, tenantId, permissionId)
                .orElseGet(() -> saveAssociation(user, tenant, permission));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_permissions.bulk_create", description = "Tempo para criar permissões diretas em lote")
    public List<UsersTenantsPermissions> assignPermissions(UUID userId, UUID tenantId, Collection<UUID> permissionIds) {
        validateUserId(userId);
        validateTenantId(tenantId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new ValidationException("Lista de permissões não pode ser vazia");
        }

        User user = requireUser(userId);
        Tenant tenant = requireTenant(tenantId);

        return permissionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(permissionId -> {
                    validatePermissionId(permissionId);
                    Permission permission = requirePermission(permissionId);
                    ensureSameLandlord(tenant, permission);
                    return usersTenantsPermissionsRepository
                            .findByUserIdAndTenantIdAndPermissionId(userId, tenantId, permissionId)
                            .orElseGet(() -> saveAssociation(user, tenant, permission));
                })
                .collect(Collectors.toList());
    }

    @Override
    @Timed(value = "users_tenants_permissions.find", description = "Tempo para buscar permissão direta específica")
    public Optional<UsersTenantsPermissions> findUserTenantPermission(UUID userId, UUID tenantId, UUID permissionId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validatePermissionId(permissionId);
        return usersTenantsPermissionsRepository.findByUserIdAndTenantIdAndPermissionId(userId, tenantId, permissionId);
    }

    @Override
    @Timed(value = "users_tenants_permissions.list", description = "Tempo para listar permissões diretas por usuário e tenant")
    public List<UsersTenantsPermissions> findPermissionsByUserAndTenant(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsPermissionsRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    @Timed(value = "users_tenants_permissions.list", description = "Tempo para listar permissões diretas por usuário")
    public List<UsersTenantsPermissions> findPermissionsByUser(UUID userId) {
        validateUserId(userId);
        return usersTenantsPermissionsRepository.findByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_permissions.delete", description = "Tempo para remover permissão direta")
    public boolean removeUserTenantPermission(UUID userId, UUID tenantId, UUID permissionId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validatePermissionId(permissionId);
        if (!usersTenantsPermissionsRepository.existsByUserIdAndTenantIdAndPermissionId(userId, tenantId, permissionId)) {
            return false;
        }
        usersTenantsPermissionsRepository.deleteByUserIdAndTenantIdAndPermissionId(userId, tenantId, permissionId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_permissions.bulk_delete", description = "Tempo para remover permissões diretas em lote")
    public boolean removePermissions(UUID userId, UUID tenantId, Collection<UUID> permissionIds) {
        validateUserId(userId);
        validateTenantId(tenantId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return false;
        }
        boolean removedAny = false;
        for (UUID permissionId : permissionIds) {
            if (permissionId == null) {
                continue;
            }
            validatePermissionId(permissionId);
            if (usersTenantsPermissionsRepository.existsByUserIdAndTenantIdAndPermissionId(userId, tenantId, permissionId)) {
                usersTenantsPermissionsRepository.deleteByUserIdAndTenantIdAndPermissionId(userId, tenantId, permissionId);
                removedAny = true;
            }
        }
        return removedAny;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAllPermissionsByUserAndTenant(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        if (!usersTenantsPermissionsRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            return false;
        }
        usersTenantsPermissionsRepository.deleteByUserIdAndTenantId(userId, tenantId);
        return true;
    }

    @Override
    public boolean existsUserTenantPermission(UUID userId, UUID tenantId, UUID permissionId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validatePermissionId(permissionId);
        return usersTenantsPermissionsRepository.existsByUserIdAndTenantIdAndPermissionId(userId, tenantId, permissionId);
    }

    @Override
    public boolean existsPermissionsByUserAndTenant(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsPermissionsRepository.existsByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public long countPermissionsByUserAndTenant(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsPermissionsRepository.countByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public List<Object> getUserTenantPermissionsDetails(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        try {
            List<Object[]> results = usersTenantsPermissionsRepository.getUserPermissionsDetails(userId, tenantId);
            return results.stream()
                    .map(row -> Map.of(
                            "id", row[0],
                            "action", row[1],
                            "resource", row[2]
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter detalhes das permissões diretas do usuário no tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UUID> getPermissionIdsByUserAndTenant(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsPermissionsRepository.findPermissionIdsByUserAndTenant(userId, tenantId);
    }

    @Override
    public List<UUID> getPermissionIdsByUser(UUID userId) {
        validateUserId(userId);
        return usersTenantsPermissionsRepository.findPermissionIdsByUser(userId);
    }

    private UsersTenantsPermissions saveAssociation(User user, Tenant tenant, Permission permission) {
        UsersTenantsPermissions association = UsersTenantsPermissions.createNew(user.getId(), tenant.getId(), permission.getId());
        association.setUser(user);
        association.setTenant(tenant);
        association.setPermission(permission);
        return usersTenantsPermissionsRepository.save(association);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + userId));
    }

    private Tenant requireTenant(UUID tenantId) {
        return tenantRepository.findByIdWithLandlord(tenantId)
                .orElseGet(() -> tenantRepository.findById(tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado com ID: " + tenantId)));
    }

    private Permission requirePermission(UUID permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada com ID: " + permissionId));
    }

    private void ensureSameLandlord(Tenant tenant, Permission permission) {
        if (tenant.getLandlord() == null || permission.getLandlord() == null) {
            throw new ValidationException("Tenant e permissão devem estar associados a landlords");
        }
        if (!tenant.getLandlord().getId().equals(permission.getLandlord().getId())) {
            throw new ValidationException("Tenant e permissão pertencem a landlords diferentes");
        }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("ID do usuário não pode ser nulo");
        }
    }

    private void validateTenantId(UUID tenantId) {
        if (tenantId == null) {
            throw new ValidationException("ID do tenant não pode ser nulo");
        }
    }

    private void validatePermissionId(UUID permissionId) {
        if (permissionId == null) {
            throw new ValidationException("ID da permissão não pode ser nulo");
        }
    }
}
