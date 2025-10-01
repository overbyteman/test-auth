package com.seccreto.service.auth.repository.users;

import com.seccreto.service.auth.model.users.User;

import java.util.List;
import java.util.Optional;

/**
 * Abstração de repositório para a entidade User, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    List<User> findAll();
    List<User> findByName(String name);
    Optional<User> findByEmail(String email);
    User update(User user);
    boolean deleteById(Long id);
    boolean existsById(Long id);
    long count();
    void clear();
    
    // Métodos adicionais para controllers
    List<User> findUsersByTenant(Long tenantId);
    List<User> search(String query);
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

