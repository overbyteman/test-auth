package com.seccreto.service.auth.service.roles;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.api.dto.roles.RoleResponse;
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
    Role createRole(UUID landlordId, String code, String name, String description);
    List<Role> listRoles(UUID landlordId);
    Optional<Role> findRoleById(UUID landlordId, UUID id);
    Optional<Role> findRoleByCode(UUID landlordId, String code);
    Role updateRole(UUID landlordId, UUID id, String name, String description);
    boolean deleteRole(UUID landlordId, UUID id);
    boolean existsRoleById(UUID landlordId, UUID id);
    boolean existsRoleByCode(UUID landlordId, String code);
    long countRoles(UUID landlordId);
    
    // Operações de busca
    List<Role> searchRoles(UUID landlordId, String query);
    Pagination<RoleResponse> searchRoles(UUID landlordId, SearchQuery searchQuery);
    
    // Operações de permissões
    List<Object> getRolePermissions(UUID landlordId, UUID roleId);
    boolean roleHasPermission(UUID landlordId, UUID roleId, String action, String resource);
    long countRolePermissions(UUID landlordId, UUID roleId);
    
    // Operações de usuários
    List<Object> getRoleUsers(UUID landlordId, UUID roleId);
    long countRoleUsers(UUID landlordId, UUID roleId);
}