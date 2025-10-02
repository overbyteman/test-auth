package com.seccreto.service.auth.repository.users;

import com.seccreto.service.auth.model.users.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração de repositório para a entidade User, permitindo trocar implementação (in-memory, JPA, etc.).
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    List<User> findAll();
    List<User> findByName(String name);
    Optional<User> findByEmail(String email);
    User update(User user);
    boolean deleteById(UUID id);
    boolean existsById(UUID id);
    long count();
    void clear();
    
    // Métodos adicionais para controllers
    List<User> findUsersByTenant(UUID tenantId);
    List<User> search(String query);
    long countActiveUsers();
    long countSuspendedUsers();
    long countUsersCreatedToday();
    long countUsersCreatedThisWeek();
    long countUsersCreatedThisMonth();
    long countUsersByTenant(UUID tenantId);
    long countActiveUsersByTenant(UUID tenantId);
    long countUsersInPeriod(String startDate, String endDate);
    long countActiveUsersInPeriod(String startDate, String endDate);
    List<User> findTopActiveUsers(int limit);
}