package com.seccreto.service.auth.service.roles;

import com.seccreto.service.auth.model.roles.Role;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstração da camada de serviço para operações de role.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 */
public interface RoleService {
    Role createRole(String name, String description);
    List<Role> listAllRoles();
    Optional<Role> findRoleById(Long id);
    List<Role> findRolesByName(String name);
    Optional<Role> findRoleByNameExact(String name);
    Role updateRole(Long id, String name, String description);
    boolean deleteRole(Long id);
    boolean existsRoleById(Long id);
    boolean existsRoleByName(String name);
    long countRoles();
    
    // Métodos adicionais para controllers
    List<Role> searchRoles(String query);
    Map<String, Long> getRoleDistribution();
}
