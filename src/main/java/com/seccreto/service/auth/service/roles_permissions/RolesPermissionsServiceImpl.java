package com.seccreto.service.auth.service.roles_permissions;

import com.seccreto.service.auth.model.roles_permissions.RolesPermissions;
import com.seccreto.service.auth.repository.roles_permissions.RolesPermissionsRepository;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementação da camada de serviço contendo regras de negócio para relacionamentos role-permissão.
 * Aplica SRP e DIP com transações declarativas.
 * 
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a RBAC (Role-Based Access Control)
 * - Relacionamentos many-to-many
 * - Operações em lote
 */
@Service
@Transactional(readOnly = true)
public class RolesPermissionsServiceImpl implements RolesPermissionsService {

    private final RolesPermissionsRepository rolesPermissionsRepository;

    public RolesPermissionsServiceImpl(RolesPermissionsRepository rolesPermissionsRepository) {
        this.rolesPermissionsRepository = rolesPermissionsRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles_permissions.assign", description = "Time taken to assign permission to role")
    public RolesPermissions assignPermissionToRole(Long roleId, Long permissionId) {
        validateRoleId(roleId);
        validatePermissionId(permissionId);
        
        // Verificar se já existe o relacionamento (idempotência)
        Optional<RolesPermissions> existing = rolesPermissionsRepository.findByRoleIdAndPermissionId(roleId, permissionId);
        if (existing.isPresent()) {
            return existing.get(); // Retorna o relacionamento existente (idempotência)
        }
        
        RolesPermissions rolesPermissions = RolesPermissions.createNew(roleId, permissionId);
        RolesPermissions savedRelation = rolesPermissionsRepository.save(rolesPermissions);
        return savedRelation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles_permissions.create", description = "Time taken to create role-permission association")
    public RolesPermissions createAssociation(Long roleId, Long permissionId) {
        return assignPermissionToRole(roleId, permissionId);
    }

    @Override
    public List<RolesPermissions> listAllRolePermissions() {
        return rolesPermissionsRepository.findAll();
    }

    @Override
    public Optional<RolesPermissions> findRolePermission(Long roleId, Long permissionId) {
        validateRoleId(roleId);
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.findByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    public List<RolesPermissions> findPermissionsByRole(Long roleId) {
        validateRoleId(roleId);
        return rolesPermissionsRepository.findByRoleId(roleId);
    }

    @Override
    public List<RolesPermissions> findRolesByPermission(Long permissionId) {
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.findByPermissionId(permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles_permissions.remove", description = "Time taken to remove permission from role")
    public boolean removePermissionFromRole(Long roleId, Long permissionId) {
        validateRoleId(roleId);
        validatePermissionId(permissionId);
        
        // Verificar se o relacionamento existe antes de tentar remover (idempotência)
        if (!rolesPermissionsRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            return false; // Relacionamento já não existe (idempotência)
        }
        
        boolean removed = rolesPermissionsRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        return removed;
    }

    @Override
    public boolean removeAssociation(Long roleId, Long permissionId) {
        return removePermissionFromRole(roleId, permissionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles_permissions.removeAllFromRole", description = "Time taken to remove all permissions from role")
    public boolean removeAllPermissionsFromRole(Long roleId) {
        validateRoleId(roleId);
        
        // Verificar se existem permissões para o role antes de tentar remover (idempotência)
        if (!rolesPermissionsRepository.existsByRoleId(roleId)) {
            return false; // Não existem permissões para o role (idempotência)
        }
        
        boolean removed = rolesPermissionsRepository.deleteByRoleId(roleId);
        return removed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "roles_permissions.removeAllFromPermission", description = "Time taken to remove all roles from permission")
    public boolean removeAllRolesFromPermission(Long permissionId) {
        validatePermissionId(permissionId);
        
        // Verificar se existem roles para a permissão antes de tentar remover (idempotência)
        if (!rolesPermissionsRepository.existsByPermissionId(permissionId)) {
            return false; // Não existem roles para a permissão (idempotência)
        }
        
        boolean removed = rolesPermissionsRepository.deleteByPermissionId(permissionId);
        return removed;
    }

    @Override
    public boolean existsRolePermission(Long roleId, Long permissionId) {
        validateRoleId(roleId);
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.existsByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    public boolean existsPermissionsForRole(Long roleId) {
        validateRoleId(roleId);
        return rolesPermissionsRepository.existsByRoleId(roleId);
    }

    @Override
    public boolean existsRolesForPermission(Long permissionId) {
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.existsByPermissionId(permissionId);
    }

    @Override
    public long countRolesByPermission(Long permissionId) {
        validatePermissionId(permissionId);
        return rolesPermissionsRepository.countByPermissionId(permissionId);
    }

    @Override
    public long countPermissionsByRole(Long roleId) {
        validateRoleId(roleId);
        return rolesPermissionsRepository.countByRoleId(roleId);
    }

    @Override
    public long countAssociations() {
        return rolesPermissionsRepository.count();
    }

    @Override
    public long countRolePermissions() {
        return rolesPermissionsRepository.count();
    }

    private void validateRoleId(Long roleId) {
        if (roleId == null) {
            throw new ValidationException("ID do role não pode ser nulo");
        }
        if (roleId <= 0) {
            throw new ValidationException("ID do role deve ser maior que zero");
        }
    }

    private void validatePermissionId(Long permissionId) {
        if (permissionId == null) {
            throw new ValidationException("ID da permissão não pode ser nulo");
        }
        if (permissionId <= 0) {
            throw new ValidationException("ID da permissão deve ser maior que zero");
        }
    }
}
