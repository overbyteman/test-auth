package com.seccreto.service.auth.service.users;

import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.repository.users.UserRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.usage.UsageService;
import io.micrometer.core.annotation.Timed;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Implementação da camada de serviço contendo regras de negócio.
 * Aplica SRP e DIP com transações declarativas.
 * Baseado na migração V1 e funções das migrações V9 e V10.
 *
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 * - Suporte a UUIDs
 * - Integração com funções de uso
 * - Criptografia de senhas com BCrypt
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMetricsService metricsService;
    private final UsageService usageService;
    private final PasswordEncoder passwordEncoder;
    
    public UserServiceImpl(UserRepository userRepository, 
                          UserMetricsService metricsService, 
                          UsageService usageService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.metricsService = metricsService;
        this.usageService = usageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users.create", description = "Time taken to create a user")
    public User createUser(String name, String email, String password) {
        validateName(name);
        validateEmail(email);

        // Verificar se já existe um usuário com este email (idempotência)
        Optional<User> existingUser = userRepository.findByEmail(email.trim());
        if (existingUser.isPresent()) {
            return existingUser.get(); // Retorna o usuário existente (idempotência)
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = User.createNew(name.trim(), email.trim(), hashedPassword);
        User savedUser = userRepository.save(user);
        
        // Registrar métricas
        metricsService.incrementUserCreated();
        
        return savedUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users.create_with_tenant", description = "Time taken to create a user with tenant association")
    public User createUserWithTenant(String name, String email, String password, UUID tenantId, UUID initialRoleId, boolean sendValidationEmail, String validationCallbackUrl) {
        validateName(name);
        validateEmail(email);
        validateId(tenantId);

        Optional<User> existingUser = userRepository.findByEmail(email.trim());
        if (existingUser.isPresent()) {
            throw new ConflictException("Usuário já existe com este email");
        }

        // Criar usuário inativo inicialmente (será ativado após validação de email)
        User user = User.createNew(name.trim(), email.trim(), password);
        user.setIsActive(false); // Inativo até validação de email
        
        User savedUser = userRepository.save(user);
        
        // TODO: Associar usuário ao tenant com role inicial
        // usersTenantsRolesService.createAssociation(savedUser.getId(), tenantId, initialRoleId);
        
        // TODO: Gerar token de validação de email
        if (sendValidationEmail) {
            String validationToken = generateEmailValidationToken(savedUser.getId());
            sendEmailValidation(savedUser.getEmail(), validationToken, validationCallbackUrl);
        }
        
        // Registrar métricas
        metricsService.incrementUserCreated();
        
        return savedUser;
    }
    
    private String generateEmailValidationToken(UUID userId) {
        // TODO: Implementar geração de token JWT para validação de email
        return "validation-token-" + userId.toString();
    }
    
    private void sendEmailValidation(String email, String token, String callbackUrl) {
        // TODO: Implementar envio de email de validação
        System.out.println("Email de validação enviado para: " + email + " com token: " + token);
    }

    @Override
    @Timed(value = "users.list", description = "Time taken to list users")
    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Timed(value = "users.find", description = "Time taken to find user by id")
    public Optional<User> findUserById(UUID id) {
        validateId(id);
        return userRepository.findById(id);
    }

    @Override
    @Timed(value = "users.find", description = "Time taken to find users by name")
    public List<User> findUsersByName(String name) {
        validateName(name);
        return userRepository.findByName(name);
    }

    @Override
    @Timed(value = "users.find", description = "Time taken to find user by email")
    public Optional<User> findByEmail(String email) {
        validateEmail(email);
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users.update", description = "Time taken to update user")
    public User updateUser(UUID id, String name, String email) {
        validateId(id);
        validateName(name);
        validateEmail(email);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        // Verificar se email já existe em outro usuário
        Optional<User> existingUser = userRepository.findByEmail(email.trim());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
            throw new ConflictException("Email já está em uso por outro usuário");
        }

        user.setName(name.trim());
        user.setEmail(email.trim());
        user.updateTimestamp();

        return userRepository.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users.delete", description = "Time taken to delete user")
    public boolean deleteUser(UUID id) {
        validateId(id);
        
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com ID: " + id);
        }

        userRepository.deleteById(id);
        boolean deleted = true;
        if (deleted) {
            metricsService.incrementUserDeleted();
        }
        
        return deleted;
    }

    @Override
    public boolean existsUserById(UUID id) {
        validateId(id);
        return userRepository.existsById(id);
    }

    @Override
    public boolean existsUserByEmail(String email) {
        validateEmail(email);
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    @Timed(value = "users.count", description = "Time taken to count users")
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User activateUser(UUID id) {
        User user = findUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        
        user.setIsActive(true);
        user.updateTimestamp();
        
        return userRepository.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User deactivateUser(UUID id) {
        User user = findUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        
        user.setIsActive(false);
        user.updateTimestamp();
        
        return userRepository.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User verifyEmail(UUID id, String token) {
        User user = findUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        
        if (token.equals(user.getEmailVerificationToken())) {
            user.setEmailVerifiedAt(java.time.LocalDateTime.now());
            user.setEmailVerificationToken(null);
            user.setIsActive(true);
            user.updateTimestamp();
            
            return userRepository.save(user);
        } else {
            throw new ValidationException("Token de verificação inválido");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User resendVerificationEmail(UUID id) {
        User user = findUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
        
        // Gerar novo token
        String newToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(newToken);
        user.updateTimestamp();
        
        return userRepository.save(user);
    }

    @Override
    public List<User> findUsersByTenant(UUID tenantId) {
        return userRepository.findUsersByTenant(tenantId);
    }

    @Override
    public List<User> searchUsers(String query) {
        return userRepository.search(query);
    }

    @Override
    public List<User> findTopActiveUsers(int limit) {
        return userRepository.findTopActiveUsers(limit);
    }

    @Override
    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }

    @Override
    public long countInactiveUsers() {
        return userRepository.countSuspendedUsers();
    }

    @Override
    public long countUsersCreatedToday() {
        return userRepository.countUsersCreatedToday();
    }

    @Override
    public long countUsersCreatedThisWeek() {
        return userRepository.countUsersCreatedThisWeek();
    }

    @Override
    public long countUsersCreatedThisMonth() {
        return userRepository.countUsersCreatedThisMonth();
    }

    @Override
    public long countUsersByTenant(UUID tenantId) {
        return userRepository.countUsersByTenant(tenantId);
    }

    @Override
    public long countActiveUsersByTenant(UUID tenantId) {
        return userRepository.countActiveUsersByTenant(tenantId);
    }

    @Override
    public long countUsersInPeriod(LocalDate startDate, LocalDate endDate) {
        return userRepository.countUsersInPeriod(startDate.toString(), endDate.toString());
    }

    @Override
    public long countActiveUsersInPeriod(LocalDate startDate, LocalDate endDate) {
        return userRepository.countActiveUsersInPeriod(startDate.toString(), endDate.toString());
    }

    @Override
    @Transactional
    public void recordUserLogin(UUID userId, UUID tenantId) {
        usageService.recordUserLogin(userId, tenantId, LocalDate.now());
    }

    @Override
    @Transactional
    public void recordUserAction(UUID userId, UUID tenantId) {
        usageService.recordUserAction(userId, tenantId, LocalDate.now(), java.time.LocalDateTime.now());
    }

    @Override
    public List<Object> getUserUsageMetrics(UUID userId, LocalDate startDate, LocalDate endDate, UUID tenantId) {
        return usageService.getUserDailyUsage(userId, startDate, endDate, tenantId)
                .stream()
                .map(usage -> (Object) usage)
                .toList();
    }

    @Override
    public List<Object> getTopActiveUsersByUsage(LocalDate startDate, LocalDate endDate, UUID tenantId, int limit) {
        return usageService.getTopActiveUsers(startDate, endDate, tenantId, limit);
    }

    @Override
    public List<Object> getUserPermissionsInTenant(UUID userId, UUID tenantId) {
        return usageService.getUserPermissionsInTenant(userId, tenantId);
    }

    @Override
    public boolean userHasPermissionInTenant(UUID userId, UUID tenantId, String action, String resource) {
        return usageService.userHasPermissionInTenant(userId, tenantId, action, resource);
    }

    @Override
    public List<Object> getUserTenantsWithRoles(UUID userId) {
        return usageService.getUserTenantsWithRoles(userId);
    }

    // Métodos de validação privados
    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("ID do usuário não pode ser nulo");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome do usuário é obrigatório");
        }
        if (name.trim().length() < 2) {
            throw new ValidationException("Nome do usuário deve ter pelo menos 2 caracteres");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email do usuário é obrigatório");
        }
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("Email deve ter um formato válido");
        }
    }
}