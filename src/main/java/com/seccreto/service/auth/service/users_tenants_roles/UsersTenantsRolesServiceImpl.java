package com.seccreto.service.auth.service.users_tenants_roles;

import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;
import com.seccreto.service.auth.repository.users_tenants_roles.UsersTenantsRolesRepository;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação da camada de serviço contendo regras de negócio para users_tenants_roles.
 * Aplica SRP e DIP com transações declarativas.
 * Baseado na migração V7.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class UsersTenantsRolesServiceImpl implements UsersTenantsRolesService {

    private final UsersTenantsRolesRepository usersTenantsRolesRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public UsersTenantsRolesServiceImpl(UsersTenantsRolesRepository usersTenantsRolesRepository, 
                                       NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.usersTenantsRolesRepository = usersTenantsRolesRepository;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.create", description = "Time taken to create a user tenant role")
    public UsersTenantsRoles createUserTenantRole(UUID userId, UUID tenantId, UUID roleId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validateRoleId(roleId);

        // Verificar se já existe a relação (idempotência)
        Optional<UsersTenantsRoles> existingRelation = usersTenantsRolesRepository.findByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
        if (existingRelation.isPresent()) {
            return existingRelation.get(); // Retorna a relação existente (idempotência)
        }

        UsersTenantsRoles usersTenantsRoles = new UsersTenantsRoles();
        usersTenantsRoles.setUserId(userId);
        usersTenantsRoles.setTenantId(tenantId);
        usersTenantsRoles.setRoleId(roleId);

        return usersTenantsRolesRepository.save(usersTenantsRoles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.create", description = "Time taken to create a user tenant role association")
    public UsersTenantsRoles createAssociation(UUID userId, UUID tenantId, UUID roleId) {
        return createUserTenantRole(userId, tenantId, roleId);
    }

    @Override
    @Timed(value = "users_tenants_roles.list", description = "Time taken to list user tenant roles")
    public List<UsersTenantsRoles> listAllUserTenantRoles() {
        return usersTenantsRolesRepository.findAll();
    }

    @Override
    @Timed(value = "users_tenants_roles.find", description = "Time taken to find user tenant role")
    public Optional<UsersTenantsRoles> findUserTenantRole(UUID userId, UUID tenantId, UUID roleId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.findByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
    }

    @Override
    @Timed(value = "users_tenants_roles.find", description = "Time taken to find roles by user and tenant")
    public List<UsersTenantsRoles> findRolesByUserAndTenant(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    @Timed(value = "users_tenants_roles.find", description = "Time taken to find users by tenant and role")
    public List<UsersTenantsRoles> findUsersByTenantAndRole(UUID tenantId, UUID roleId) {
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.findByTenantIdAndRoleId(tenantId, roleId);
    }

    @Override
    @Timed(value = "users_tenants_roles.find", description = "Time taken to find tenants by user and role")
    public List<UsersTenantsRoles> findTenantsByUserAndRole(UUID userId, UUID roleId) {
        validateUserId(userId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.findByUserIdAndRoleId(userId, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.delete", description = "Time taken to delete user tenant role")
    public boolean deleteUserTenantRole(UUID userId, UUID tenantId, UUID roleId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.deleteByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users_tenants_roles.delete", description = "Time taken to remove user tenant role association")
    public boolean removeAssociation(UUID userId, UUID tenantId, UUID roleId) {
        return deleteUserTenantRole(userId, tenantId, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAllRolesByUserAndTenant(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.deleteByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAllUsersByTenantAndRole(UUID tenantId, UUID roleId) {
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.deleteByTenantIdAndRoleId(tenantId, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAllTenantsByUserAndRole(UUID userId, UUID roleId) {
        validateUserId(userId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public boolean existsUserTenantRole(UUID userId, UUID tenantId, UUID roleId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.existsByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
    }

    @Override
    public boolean existsRolesByUserAndTenant(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.existsByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public boolean existsUsersByTenantAndRole(UUID tenantId, UUID roleId) {
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.existsByTenantIdAndRoleId(tenantId, roleId);
    }

    @Override
    public boolean existsTenantsByUserAndRole(UUID userId, UUID roleId) {
        validateUserId(userId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.existsByUserIdAndRoleId(userId, roleId);
    }

    @Override
    @Timed(value = "users_tenants_roles.count", description = "Time taken to count user tenant roles")
    public long countUserTenantRoles() {
        return usersTenantsRolesRepository.count();
    }

    @Override
    @Timed(value = "users_tenants_roles.count", description = "Time taken to count associations")
    public long countAssociations() {
        return usersTenantsRolesRepository.count();
    }

    @Override
    public long countRolesByUserAndTenant(UUID userId, UUID tenantId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        return usersTenantsRolesRepository.countByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public long countUsersByTenantAndRole(UUID tenantId, UUID roleId) {
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.countByTenantIdAndRoleId(tenantId, roleId);
    }

    @Override
    public long countTenantsByUserAndRole(UUID userId, UUID roleId) {
        validateUserId(userId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.countByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public boolean userHasRoleInTenant(UUID userId, UUID tenantId, UUID roleId) {
        validateUserId(userId);
        validateTenantId(tenantId);
        validateRoleId(roleId);
        return usersTenantsRolesRepository.existsByUserIdAndTenantIdAndRoleId(userId, tenantId, roleId);
    }

    @Override
    public boolean userHasRoleInTenantByRoleName(UUID userId, UUID tenantId, String roleName) {
        try {
            String sql = """
                SELECT COUNT(1)
                FROM users_tenants_roles utr
                JOIN roles r ON utr.role_id = r.id
                WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId AND r.name = :roleName
                """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId)
                    .addValue("roleName", roleName);
            
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar role do usuário no tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getUserTenantRolesDetails(UUID userId, UUID tenantId) {
        try {
            String sql = """
                SELECT r.id, r.name, r.description
                FROM roles r
                JOIN users_tenants_roles utr ON r.id = utr.role_id
                WHERE utr.user_id = :userId AND utr.tenant_id = :tenantId
                ORDER BY r.name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("tenantId", tenantId);
            
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> 
                java.util.Map.of(
                    "id", rs.getObject("id", UUID.class),
                    "name", rs.getString("name"),
                    "description", rs.getString("description")
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter detalhes dos roles do usuário no tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getTenantRoleUsersDetails(UUID tenantId, UUID roleId) {
        try {
            String sql = """
                SELECT u.id, u.name, u.email, u.is_active
                FROM users u
                JOIN users_tenants_roles utr ON u.id = utr.user_id
                WHERE utr.tenant_id = :tenantId AND utr.role_id = :roleId
                ORDER BY u.name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("roleId", roleId);
            
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> 
                java.util.Map.of(
                    "id", rs.getObject("id", UUID.class),
                    "name", rs.getString("name"),
                    "email", rs.getString("email"),
                    "isActive", rs.getBoolean("is_active")
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter detalhes dos usuários do tenant com role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> getUserRoleTenantsDetails(UUID userId, UUID roleId) {
        try {
            String sql = """
                SELECT t.id, t.name, t.config
                FROM tenants t
                JOIN users_tenants_roles utr ON t.id = utr.tenant_id
                WHERE utr.user_id = :userId AND utr.role_id = :roleId
                ORDER BY t.name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("roleId", roleId);
            
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> 
                java.util.Map.of(
                    "id", rs.getObject("id", UUID.class),
                    "name", rs.getString("name"),
                    "config", rs.getString("config")
                )
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter detalhes dos tenants do usuário com role: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> findRoleNamesByUser(UUID userId) {
        try {
            String sql = """
                SELECT DISTINCT r.name
                FROM roles r
                JOIN users_tenants_roles utr ON r.id = utr.role_id
                WHERE utr.user_id = :userId
                ORDER BY r.name
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            
            return namedParameterJdbcTemplate.queryForList(sql, params, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter nomes dos roles do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> findPermissionNamesByUser(UUID userId) {
        try {
            String sql = """
                SELECT DISTINCT p.action || ':' || p.resource as permission
                FROM permissions p
                JOIN roles_permissions rp ON p.id = rp.permission_id
                JOIN users_tenants_roles utr ON rp.role_id = utr.role_id
                WHERE utr.user_id = :userId
                ORDER BY permission
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            
            return namedParameterJdbcTemplate.queryForList(sql, params, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter nomes das permissões do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public long countRolesByUser(UUID userId) {
        try {
            String sql = """
                SELECT COUNT(DISTINCT utr.role_id)
                FROM users_tenants_roles utr
                WHERE utr.user_id = :userId
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar roles do usuário: " + e.getMessage(), e);
        }
    }

    @Override
    public long countPermissionsByUser(UUID userId) {
        try {
            String sql = """
                SELECT COUNT(DISTINCT p.id)
                FROM permissions p
                JOIN roles_permissions rp ON p.id = rp.permission_id
                JOIN users_tenants_roles utr ON rp.role_id = utr.role_id
                WHERE utr.user_id = :userId
                """;
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao contar permissões do usuário: " + e.getMessage(), e);
        }
    }

    // Métodos de validação privados
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

    private void validateRoleId(UUID roleId) {
        if (roleId == null) {
            throw new ValidationException("ID do role não pode ser nulo");
        }
    }
}