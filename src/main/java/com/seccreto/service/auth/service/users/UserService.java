package com.seccreto.service.auth.service.users;

import com.seccreto.service.auth.model.users.User;

import java.util.List;
import java.util.Optional;

/**
 * Abstração da camada de serviço para operações de usuário.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 */
public interface UserService {
    User createUser(String name, String email);
    List<User> listAllUsers();
    Optional<User> findUserById(Long id);
    List<User> findUsersByName(String name);
    Optional<User> findByEmail(String email);
    User updateUser(Long id, String name, String email);
    boolean deleteUser(Long id);
    boolean existsUserById(Long id);
    boolean existsUserByEmail(String email);
    long countUsers();
    
    // Métodos adicionais para controllers
    List<User> findUsersByTenant(Long tenantId);
    List<User> searchUsers(String query);
    User suspendUser(Long id);
    User activateUser(Long id);
    long countActiveUsers();
    long countSuspendedUsers();
    long countUsersCreatedToday();
    long countUsersCreatedThisWeek();
    long countUsersCreatedThisMonth();
    long countUsersByTenant(Long tenantId);
    long countActiveUsersByTenant(Long tenantId);
    long countUsersInPeriod(String startDate, String endDate);
    long countActiveUsersInPeriod(String startDate, String endDate);
    List<User> findTopActiveUsers(int limit);
}
