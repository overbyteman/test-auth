package com.seccreto.service.auth.service.users_tenants_roles;

import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;
import com.seccreto.service.auth.repository.users_tenants_roles.UsersTenantsRolesRepository;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementação da camada de serviço contendo regras de negócio para relacionamentos user-tenant-role.
 * Aplica SRP e DIP com transações declarativas.
 * 
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a multi-tenancy
 * - Relacionamentos many-to-many complexos
 * - Operações em lote
 */
@Service
@Transactional(readOnly = true)
public class UsersTenantsRolesServiceImpl implements UsersTenantsRolesService {

    private final UsersTenantsRolesRepository usersTenantsRolesRepository;

    public UsersTenantsRolesServiceImpl(UsersTenantsRolesRepository usersTenantsRolesRepository) {
        this.usersTenantsRolesRepository = usersTenantsRolesRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.assign", description = "Time taken to assign role to user in tenant")
    public UsersTenantsRoles assignRoleToUserInTenant(Long userId, Long tenantId, Long roleId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validateRoleId(roleId);
        
        // Verificar se já existe o relacionamento (idempotência)
        Optional<UsersTenantsRoles> existing = usersTenantsRolesRepository.findByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
        if (existing.isPresent()) {
            return existing.get(); // Retorna o relacionamento existente (idempotência)
        }
        
        UsersTenantsRoles usersTenantsRoles = UsersTenantsRoles.createNew(userId, tenantId, roleId);
        UsersTenantsRoles savedRelation = usersTenantsRolesRepository.save(usersTenantsRoles);
        return savedRelation;
    }

    @Override
    public UsersTenantsRoles createAssociation(Long userId, Long tenantId, Long roleId) {
        return assignRoleToUserInTenant(userId, tenantId, roleId);
    }

    @Override
    public boolean removeAssociation(Long userId, Long tenantId, Long roleId) {
        return removeRoleFromUserInTenant(userId, tenantId, roleId);
    }

    @Override
    public List<UsersTenantsRoles> listAllUserTenantRoles() {
        return usersTenantsRolesRepository.findAll();
    }

    @Override
    public Optional<UsersTenantsRoles> findUserTenantRole(Long userId, Long tenantId, Long roleId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.findByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
    }

    @Override
    public List<UsersTenantsRoles> findRolesByUser(Long userId) {
        validateUserId(userId);
        return usersTenantsRolesRepository.findByUserId(userId);
    }

    @Override
    public List<UsersTenantsRoles> findUsersByTenant(Long tenantId) {
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.findByTenantId(tenantId);
    }

    @Override
    public List<UsersTenantsRoles> findUsersByRole(Long roleId) {
        validateRoleId(roleId);
        return usersTenantsRolesRepository.findByRoleId(roleId);
    }

    @Override
    public List<UsersTenantsRoles> findRolesByUserAndTenant(Long userId, Long tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public List<UsersTenantsRoles> findUsersByTenantAndRole(Long tenantId, Long roleId) {
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.findByTenantIdAndRoleId(tenantId, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.remove", description = "Time taken to remove role from user in tenant")
    public boolean removeRoleFromUserInTenant(Long userId, Long tenantId, Long roleId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validateRoleId(roleId);
        
        // Verificar se o relacionamento existe antes de tentar remover (idempotência)
        if (!usersTenantsRolesRepository.existsByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId)) {
            return false; // Relacionamento já não existe (idempotência)
        }
        
        boolean removed = usersTenantsRolesRepository.deleteByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.removeAllFromUser", description = "Time taken to remove all roles from user")
    public boolean removeAllRolesFromUser(Long userId) {
        validateUserId(userId);
        
        // Verificar se existem roles para o usuário antes de tentar remover (idempotência)
        if (!usersTenantsRolesRepository.existsByUserId(userId)) {
            return false; // Não existem roles para o usuário (idempotência)
        }
        
        boolean removed = usersTenantsRolesRepository.deleteByUserId(userId);
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.removeAllFromTenant", description = "Time taken to remove all users from tenant")
    public boolean removeAllUsersFromTenant(Long tenantId) {
        validateTenantId(tenantId);
        
        // Verificar se existem usuários para o tenant antes de tentar remover (idempotência)
        if (!usersTenantsRolesRepository.existsByTenantId(tenantId)) {
            return false; // Não existem usuários para o tenant (idempotência)
        }
        
        boolean removed = usersTenantsRolesRepository.deleteByTenantId(tenantId);
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.removeAllFromRole", description = "Time taken to remove all users from role")
    public boolean removeAllUsersFromRole(Long roleId) {
        validateRoleId(roleId);
        
        // Verificar se existem usuários para o role antes de tentar remover (idempotência)
        if (!usersTenantsRolesRepository.existsByRoleId(roleId)) {
            return false; // Não existem usuários para o role (idempotência)
        }
        
        boolean removed = usersTenantsRolesRepository.deleteByRoleId(roleId);
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.removeAllFromUserInTenant", description = "Time taken to remove all roles from user in tenant")
    public boolean removeAllRolesFromUserInTenant(Long userId, Long tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        
        // Verificar se existem roles para o usuário no tenant antes de tentar remover (idempotência)
        if (!usersTenantsRolesRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            return false; // Não existem roles para o usuário no tenant (idempotência)
        }
        
        boolean removed = usersTenantsRolesRepository.deleteByUserIdAndTenantId(userId, tenantId);
        return removed;
    }

    @Override
    public boolean existsUserTenantRole(Long userId, Long tenantId, Long roleId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.existsByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
    }

    @Override
    public boolean existsRolesForUser(Long userId) {
        validateUserId(userId);
        return usersTenantsRolesRepository.existsByUserId(userId);
    }

    @Override
    public boolean existsUsersForTenant(Long tenantId) {
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.existsByTenantId(tenantId);
    }

    @Override
    public boolean existsUsersForRole(Long roleId) {
        validateRoleId(roleId);
        return usersTenantsRolesRepository.existsByRoleId(roleId);
    }

    @Override
    public boolean existsRolesForUserInTenant(Long userId, Long tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.existsByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public long countUserTenantRoles() {
        return usersTenantsRolesRepository.count();
    }

    @Override
    public long countRolesByUser(Long userId) {
        validateUserId(userId);
        return usersTenantsRolesRepository.countByUserId(userId);
    }

    @Override
    public long countUsersByTenant(Long tenantId) {
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.countByTenantId(tenantId);
    }

    @Override
    public long countUsersByRole(Long roleId) {
        validateRoleId(roleId);
        return usersTenantsRolesRepository.countByRoleId(roleId);
    }

    @Override
    public long countRolesByUserAndTenant(Long userId, Long tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.countByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public long countAssociations() {
        return usersTenantsRolesRepository.count();
    }

    @Override
    public List<String> findRoleNamesByUser(Long userId) {
        validateUserId(userId);
        return usersTenantsRolesRepository.findByUserId(userId)
                .stream()
                .map(utr -> "Role_" + utr.getRoleId()) // Placeholder - seria melhor fazer join com roles
                .distinct()
                .toList();
    }

    @Override
    public List<String> findPermissionsByUser(Long userId) {
        validateUserId(userId);
        // Implementação básica - retorna lista vazia por enquanto
        return List.of();
    }

    @Override
    public List<String> findPermissionNamesByUser(Long userId) {
        validateUserId(userId);
        // Implementação básica - retorna lista vazia por enquanto
        // Em uma implementação completa, faria join com roles_permissions e permissions
        return List.of();
    }

    @Override
    public long countPermissionsByUser(Long userId) {
        validateUserId(userId);
        // Implementação básica - retorna 0 por enquanto
        return 0;
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new ValidationException("ID do usuário não pode ser nulo");
        }
        if (userId <= 0) {
            throw new ValidationException("ID do usuário deve ser maior que zero");
        }
    }

    private void validateTenantId(Long tenantId) {
        if (tenantId == null) {
            throw new ValidationException("ID do tenant não pode ser nulo");
        }
        if (tenantId <= 0) {
            throw new ValidationException("ID do tenant deve ser maior que zero");
        }
    }

    private void validateRoleId(Long roleId) {
        if (roleId == null) {
            throw new ValidationException("ID do role não pode ser nulo");
        }
        if (roleId <= 0) {
            throw new ValidationException("ID do role deve ser maior que zero");
        }
    }
}
