package com.seccreto.service.auth.service.roles;

import com.seccreto.service.auth.model.roles.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de role.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 * Baseado na migração V4.
 */
public interface RoleService {
    
    // Operações básicas CRUD
    Role createRole(String name, String description);
    List<Role> listAllRoles();
    Optional<Role> findRoleById(UUID id);
    List<Role> findRolesByName(String name);
    Optional<Role> findRoleByNameExact(String name);
    Role updateRole(UUID id, String name, String description);
    boolean deleteRole(UUID id);
    boolean existsRoleById(UUID id);
    boolean existsRoleByName(String name);
    long countRoles();
    
    // Operações de busca
    List<Role> searchRoles(String query);
    
    // Operações de permissões
    List<Object> getRolePermissions(UUID roleId);
    boolean roleHasPermission(UUID roleId, String action, String resource);
    long countRolePermissions(UUID roleId);
    
    // Operações de usuários
    List<Object> getRoleUsers(UUID roleId);
    long countRoleUsers(UUID roleId);
}