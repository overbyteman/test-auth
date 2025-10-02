package com.seccreto.service.auth.service.users;

import com.seccreto.service.auth.model.users.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de usuário.
 * Segue o princípio da inversão de dependências (DIP): controladores dependem desta interface e não da implementação.
 * Baseado na migração V1 e funções de uso das migrações V9 e V10.
 */
public interface UserService {
    
    // Operações básicas CRUD
    User createUser(String name, String email, String password);
    User createUserWithTenant(String name, String email, String password, UUID tenantId, UUID initialRoleId, boolean sendValidationEmail, String validationCallbackUrl);
    List<User> listAllUsers();
    Optional<User> findUserById(UUID id);
    List<User> findUsersByName(String name);
    Optional<User> findByEmail(String email);
    User updateUser(UUID id, String name, String email);
    boolean deleteUser(UUID id);
    boolean existsUserById(UUID id);
    boolean existsUserByEmail(String email);
    long countUsers();
    
    // Operações de autenticação e ativação
    User activateUser(UUID id);
    User deactivateUser(UUID id);
    User verifyEmail(UUID id, String token);
    User resendVerificationEmail(UUID id);
    
    // Operações de busca e filtros
    List<User> findUsersByTenant(UUID tenantId);
    List<User> searchUsers(String query);
    List<User> findTopActiveUsers(int limit);
    
    // Métricas e estatísticas
    long countActiveUsers();
    long countInactiveUsers();
    long countUsersCreatedToday();
    long countUsersCreatedThisWeek();
    long countUsersCreatedThisMonth();
    long countUsersByTenant(UUID tenantId);
    long countActiveUsersByTenant(UUID tenantId);
    long countUsersInPeriod(LocalDate startDate, LocalDate endDate);
    long countActiveUsersInPeriod(LocalDate startDate, LocalDate endDate);
    
    // Operações de uso (baseadas nas funções das migrações)
    void recordUserLogin(UUID userId, UUID tenantId);
    void recordUserAction(UUID userId, UUID tenantId);
    List<Object> getUserUsageMetrics(UUID userId, LocalDate startDate, LocalDate endDate, UUID tenantId);
    List<Object> getTopActiveUsersByUsage(LocalDate startDate, LocalDate endDate, UUID tenantId, int limit);
    
    // Operações de permissões (baseadas nas funções das migrações)
    List<Object> getUserPermissionsInTenant(UUID userId, UUID tenantId);
    boolean userHasPermissionInTenant(UUID userId, UUID tenantId, String action, String resource);
    List<Object> getUserTenantsWithRoles(UUID userId);
}