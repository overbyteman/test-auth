package com.seccreto.service.auth.service.users.assignments;

import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.permissions.Permission;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.model.tenants.Tenant;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.repository.roles.RoleRepository;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.tenants.TenantService;
import com.seccreto.service.auth.service.users.UserService;
import com.seccreto.service.auth.service.users.assignments.UserAssignmentService.AssignmentResult;
import com.seccreto.service.auth.service.users_tenants_permissions.UsersTenantsPermissionsService;
import com.seccreto.service.auth.service.users_tenants_roles.UsersTenantsRolesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAssignmentServiceImplTest {

    @Mock
    private UsersTenantsRolesService usersTenantsRolesService;

    @Mock
    private UsersTenantsPermissionsService usersTenantsPermissionsService;

    @Mock
    private UserService userService;

    @Mock
    private TenantService tenantService;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserAssignmentServiceImpl userAssignmentService;

    private Landlord landlord;
    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        landlord = Landlord.createNew("Alliance HQ", null);
        landlord.setId(UUID.randomUUID());
        tenant = Tenant.createNew("Academia Central", null, landlord);
        tenant.setId(UUID.randomUUID());
        user = User.createNew("Bruce", "bruce@dojo.com", "hash");
        user.setId(UUID.randomUUID());
    }

    @Test
    void assignRolesShouldPropagatePermissions() {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        Role role = Role.createNew("sensei", "SENSEI", "Instrutor", landlord);
        role.setId(roleId);

        Permission permission = Permission.createNew("manage", "students", landlord);
        permission.setId(permissionId);

        RolesPermissions association = RolesPermissions.of(role, permission);
        Set<RolesPermissions> associations = new HashSet<>();
        associations.add(association);
        role.setRolePermissions(associations);

        when(userService.findUserById(user.getId())).thenReturn(Optional.of(user));
        when(tenantService.findTenantById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(roleRepository.findByIdWithTenantAndPermissions(roleId)).thenReturn(Optional.of(role));
        when(usersTenantsRolesService.existsUserTenantRole(user.getId(), tenant.getId(), roleId)).thenReturn(false);
        when(usersTenantsPermissionsService.existsUserTenantPermission(user.getId(), tenant.getId(), permissionId)).thenReturn(false);
        when(usersTenantsPermissionsService.createUserTenantPermission(user.getId(), tenant.getId(), permissionId))
                .thenReturn(null);

        AssignmentResult result = userAssignmentService.assignRoles(user.getId(), tenant.getId(), List.of(roleId));

        assertThat(result.newlyAssignedRoleIds()).containsExactly(roleId);
        assertThat(result.propagatedPermissionIds()).containsExactly(permissionId);
        assertThat(result.newlyAssignedPermissionIds()).containsExactly(permissionId);
        assertThat(result.requestedPermissionIds()).isEmpty();

        verify(usersTenantsRolesService).createAssociation(user.getId(), tenant.getId(), roleId);
        verify(usersTenantsPermissionsService).createUserTenantPermission(user.getId(), tenant.getId(), permissionId);
    }

    @Test
    void assignRolesShouldNotReportAlreadyExistingPropagatedPermissions() {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        Role role = Role.createNew("sensei", "SENSEI", "Instrutor", landlord);
        role.setId(roleId);

        Permission permission = Permission.createNew("manage", "students", landlord);
        permission.setId(permissionId);

        RolesPermissions association = RolesPermissions.of(role, permission);
        Set<RolesPermissions> associations = new HashSet<>();
        associations.add(association);
        role.setRolePermissions(associations);

        when(userService.findUserById(user.getId())).thenReturn(Optional.of(user));
        when(tenantService.findTenantById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(roleRepository.findByIdWithTenantAndPermissions(roleId)).thenReturn(Optional.of(role));
        when(usersTenantsRolesService.existsUserTenantRole(user.getId(), tenant.getId(), roleId)).thenReturn(false);
        when(usersTenantsPermissionsService.existsUserTenantPermission(user.getId(), tenant.getId(), permissionId))
                .thenReturn(true);

        AssignmentResult result = userAssignmentService.assignRoles(user.getId(), tenant.getId(), List.of(roleId));

        assertThat(result.newlyAssignedRoleIds()).containsExactly(roleId);
        assertThat(result.newlyAssignedPermissionIds()).isEmpty();
        assertThat(result.alreadyAssignedPermissionIds()).containsExactly(permissionId);
        assertThat(result.propagatedPermissionIds()).isEmpty();
    }

    @Test
    void assignRolesShouldFailWhenRoleBelongsToAnotherLandlord() {
        UUID otherRoleId = UUID.randomUUID();
        Landlord otherLandlord = Landlord.builder().id(UUID.randomUUID()).name("Outra Rede").build();
        Role role = Role.createNew("outsider", "OUTSIDER", "Role externo", otherLandlord);
        role.setId(otherRoleId);

        when(userService.findUserById(user.getId())).thenReturn(Optional.of(user));
        when(tenantService.findTenantById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(roleRepository.findByIdWithTenantAndPermissions(otherRoleId)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> userAssignmentService.assignRoles(user.getId(), tenant.getId(), List.of(otherRoleId)))
                .isInstanceOf(ValidationException.class);
    }
}
