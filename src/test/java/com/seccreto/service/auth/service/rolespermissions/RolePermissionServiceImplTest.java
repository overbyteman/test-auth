package com.seccreto.service.auth.service.rolespermissions;

import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.repository.permissions.PermissionRepository;
import com.seccreto.service.auth.repository.policies.PolicyRepository;
import com.seccreto.service.auth.repository.roles.RoleRepository;
import com.seccreto.service.auth.repository.roles_permissions.RolesPermissionsRepository;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private RolesPermissionsRepository rolesPermissionsRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private RolePermissionServiceImpl service;

    private Landlord landlord;
    private Role role;
    private Permission permission;
    private Policy defaultPolicy;

    @BeforeEach
    void setup() {
        landlord = Landlord.builder()
                .id(UUID.randomUUID())
                .name("Landlord")
                .build();

        role = Role.builder()
                .id(UUID.randomUUID())
                .code("role-code")
                .name("Role Name")
                .landlord(landlord)
                .build();

        defaultPolicy = Policy.builder()
                .id(UUID.randomUUID())
                .code("default")
                .name("Default Policy")
                .effect(PolicyEffect.ALLOW)
                .tenant(null)
                .build();

        permission = Permission.builder()
                .id(UUID.randomUUID())
                .action("read")
                .resource("members")
                .landlord(landlord)
                .policy(defaultPolicy)
                .build();
    }

    @Test
    void shouldAttachPermissionUsingDefaultPolicyWhenInherit() {
        UUID landlordId = landlord.getId();

        when(roleRepository.findByIdWithTenantAndPermissions(role.getId())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));
        when(rolesPermissionsRepository.findByRoleIdAndPermissionId(role.getId(), permission.getId()))
                .thenReturn(Optional.empty());
        when(rolesPermissionsRepository.save(any(RolesPermissions.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RolesPermissions association = service.attachPermission(landlordId, role.getId(), permission.getId(), null, true);

        ArgumentCaptor<RolesPermissions> captor = ArgumentCaptor.forClass(RolesPermissions.class);
        verify(rolesPermissionsRepository).save(captor.capture());

        RolesPermissions saved = captor.getValue();
        assertThat(saved.getPolicy()).isEqualTo(defaultPolicy);
        assertThat(association.getPolicy()).isEqualTo(defaultPolicy);
    }

    @Test
    void shouldAttachPermissionWithExplicitPolicy() {
        UUID landlordId = landlord.getId();
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .landlord(landlord)
                .build();

        Policy overridePolicy = Policy.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .code("override")
                .name("Override Policy")
                .effect(PolicyEffect.DENY)
                .build();

        when(roleRepository.findByIdWithTenantAndPermissions(role.getId())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));
        when(policyRepository.findById(overridePolicy.getId())).thenReturn(Optional.of(overridePolicy));
        when(rolesPermissionsRepository.findByRoleIdAndPermissionId(role.getId(), permission.getId()))
                .thenReturn(Optional.empty());
        when(rolesPermissionsRepository.save(any(RolesPermissions.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RolesPermissions association = service.attachPermission(landlordId, role.getId(), permission.getId(), overridePolicy.getId(), false);

        assertThat(association.getPolicy()).isEqualTo(overridePolicy);
        verify(policyRepository).findById(overridePolicy.getId());
    }

    @Test
    void shouldUpdateExistingAssociationClearingPolicyWhenNotInherited() {
        UUID landlordId = landlord.getId();
        RolesPermissions existing = RolesPermissions.of(role, permission, defaultPolicy);

        when(rolesPermissionsRepository.findByRoleIdAndPermissionId(role.getId(), permission.getId()))
                .thenReturn(Optional.of(existing));
        when(roleRepository.findByIdWithTenantAndPermissions(role.getId())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));
        when(rolesPermissionsRepository.save(any(RolesPermissions.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RolesPermissions association = service.updatePermissionPolicy(landlordId, role.getId(), permission.getId(), null, false);

        assertThat(association.getPolicy()).isNull();
        verify(rolesPermissionsRepository).save(existing);
    }

    @Test
    void shouldDetachAssociation() {
        UUID landlordId = landlord.getId();
        RolesPermissions existing = RolesPermissions.of(role, permission, defaultPolicy);

        when(roleRepository.findByIdWithTenantAndPermissions(role.getId())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));
        when(rolesPermissionsRepository.findByRoleIdAndPermissionId(role.getId(), permission.getId()))
                .thenReturn(Optional.of(existing));
        when(rolesPermissionsRepository.deleteByRoleIdAndPermissionId(role.getId(), permission.getId()))
                .thenReturn(1);

        boolean removed = service.detachPermission(landlordId, role.getId(), permission.getId());

        assertThat(removed).isTrue();
        verify(rolesPermissionsRepository).deleteByRoleIdAndPermissionId(role.getId(), permission.getId());
    }

    @Test
    void shouldValidateLandlordForPolicyOverride() {
                UUID landlordId = landlord.getId();
                UUID otherLandlordId = UUID.randomUUID();
        Landlord otherLandlord = Landlord.builder()
                .id(otherLandlordId)
                .name("Other Landlord")
                .build();

        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .landlord(otherLandlord)
                .build();

        Policy policy = Policy.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .build();

        when(roleRepository.findByIdWithTenantAndPermissions(role.getId())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));
        when(policyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        assertThatThrownBy(() -> service.attachPermission(landlordId, role.getId(), permission.getId(), policy.getId(), false))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldFailWhenRoleNotFound() {
        UUID landlordId = landlord.getId();
        when(roleRepository.findByIdWithTenantAndPermissions(role.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attachPermission(landlordId, role.getId(), permission.getId(), null, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFailWhenPermissionNotFound() {
        UUID landlordId = landlord.getId();
        when(roleRepository.findByIdWithTenantAndPermissions(role.getId())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attachPermission(landlordId, role.getId(), permission.getId(), null, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
