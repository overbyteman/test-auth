package com.seccreto.service.auth.service.rolespermissions;

import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.repository.permissions.PermissionRepository;
import com.seccreto.service.auth.repository.policies.PolicyRepository;
import com.seccreto.service.auth.repository.roles.RoleRepository;
import com.seccreto.service.auth.repository.roles_permissions.RolesPermissionsRepository;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RolesPermissionsRepository rolesPermissionsRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PolicyRepository policyRepository;

    public RolePermissionServiceImpl(RolesPermissionsRepository rolesPermissionsRepository,
                                     RoleRepository roleRepository,
                                     PermissionRepository permissionRepository,
                                     PolicyRepository policyRepository) {
        this.rolesPermissionsRepository = rolesPermissionsRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.policyRepository = policyRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.permissions.attach", description = "Tempo para anexar permissao a role")
    public RolesPermissions attachPermission(UUID landlordId, UUID roleId, UUID permissionId, UUID policyId, boolean inheritDefaultPolicy) {
        validateLandlordId(landlordId);
        Role role = requireRole(landlordId, roleId);
        Permission permission = requirePermission(landlordId, permissionId);
        Policy policy = resolvePolicy(landlordId, policyId, permission, inheritDefaultPolicy);

        Optional<RolesPermissions> existing = rolesPermissionsRepository.findByRoleIdAndPermissionId(roleId, permissionId);
        if (existing.isPresent()) {
            RolesPermissions association = existing.get();
            applyPolicy(association, policy);
            syncAggregates(association);
            return rolesPermissionsRepository.save(association);
        }

        RolesPermissions association = RolesPermissions.of(role, permission, policy);
        syncAggregates(association);
        return rolesPermissionsRepository.save(association);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.permissions.update", description = "Tempo para atualizar policy da associacao role-permission")
    public RolesPermissions updatePermissionPolicy(UUID landlordId, UUID roleId, UUID permissionId, UUID policyId, boolean inheritDefaultPolicy) {
        validateLandlordId(landlordId);
        RolesPermissions association = rolesPermissionsRepository.findByRoleIdAndPermissionId(roleId, permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Associação role-permission não encontrada"));

        Role role = requireRole(landlordId, roleId);
        Permission permission = requirePermission(landlordId, permissionId);
        Policy policy = resolvePolicy(landlordId, policyId, permission, inheritDefaultPolicy);

        association.setRole(role);
        association.setPermission(permission);
        applyPolicy(association, policy);
        syncAggregates(association);
        return rolesPermissionsRepository.save(association);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles.permissions.detach", description = "Tempo para remover permissao de role")
    public boolean detachPermission(UUID landlordId, UUID roleId, UUID permissionId) {
        validateLandlordId(landlordId);
        requireRole(landlordId, roleId);
        requirePermission(landlordId, permissionId);

        Optional<RolesPermissions> association = rolesPermissionsRepository.findByRoleIdAndPermissionId(roleId, permissionId);
        if (association.isEmpty()) {
            return false;
        }

        RolesPermissions link = association.get();
        link.getRole().getRolePermissions().removeIf(rp -> Objects.equals(rp.getId(), link.getId()));
        link.getPermission().getRolePermissions().removeIf(rp -> Objects.equals(rp.getId(), link.getId()));
        rolesPermissionsRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        return true;
    }

    @Override
    public Optional<RolesPermissions> findAssociation(UUID landlordId, UUID roleId, UUID permissionId) {
        validateLandlordId(landlordId);
        return rolesPermissionsRepository.findByRoleIdAndPermissionId(roleId, permissionId)
                .filter(association -> isSameLandlord(association, landlordId));
    }

    @Override
    public List<RolesPermissions> listRolePermissions(UUID landlordId, UUID roleId) {
        validateLandlordId(landlordId);
        Role role = requireRole(landlordId, roleId);
        return rolesPermissionsRepository.findByRoleId(role.getId());
    }

    private void applyPolicy(RolesPermissions association, Policy policy) {
        if (policy == null) {
            association.clearPolicy();
        } else {
            association.attachPolicy(policy);
        }
    }

    private Role requireRole(UUID landlordId, UUID roleId) {
        return roleRepository.findByIdWithTenantAndPermissions(roleId)
                .filter(role -> role.getLandlord() != null && landlordId.equals(role.getLandlord().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role não encontrado para o landlord informado (landlordId=" + landlordId + ", roleId=" + roleId + ")"));
    }

    private Permission requirePermission(UUID landlordId, UUID permissionId) {
        return permissionRepository.findById(permissionId)
                .filter(permission -> permission.getLandlord() != null && landlordId.equals(permission.getLandlord().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permissão não encontrada para o landlord informado (landlordId=" + landlordId + ", permissionId=" + permissionId + ")"));
    }

    private Policy resolvePolicy(UUID landlordId, UUID policyId, Permission permission, boolean inheritDefault) {
        if (policyId == null) {
            return inheritDefault ? permission.getPolicy() : null;
        }

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy não encontrada com ID: " + policyId));

        Landlord landlord = policy.getTenant() != null ? policy.getTenant().getLandlord() : null;
        if (landlord == null || !landlordId.equals(landlord.getId())) {
            throw new ValidationException("Policy informada não pertence ao landlord informado");
        }

        return policy;
    }

    private void validateLandlordId(UUID landlordId) {
        if (landlordId == null) {
            throw new ValidationException("ID do landlord é obrigatório");
        }
    }

    private boolean isSameLandlord(RolesPermissions association, UUID landlordId) {
        Role role = association.getRole();
        Permission permission = association.getPermission();
        if (role == null || permission == null) {
            return false;
        }
        UUID roleLandlordId = role.getLandlord() != null ? role.getLandlord().getId() : null;
        UUID permissionLandlordId = permission.getLandlord() != null ? permission.getLandlord().getId() : null;
        return landlordId.equals(roleLandlordId) && landlordId.equals(permissionLandlordId);
    }

    private void syncAggregates(RolesPermissions association) {
        if (association.getRole() != null) {
            association.getRole().getRolePermissions().add(association);
        }
        if (association.getPermission() != null) {
            association.getPermission().getRolePermissions().add(association);
        }
    }
}
