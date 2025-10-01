package com.seccreto.service.auth.service.users;

import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.repository.users.UserRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementação da camada de serviço contendo regras de negócio.
 * Aplica SRP e DIP com transações declarativas.
 *
 * Características de implementação sênior:
 * - Métricas de negócio
 * - Timing automático
 * - Tratamento de exceções específicas
 * - Transações otimizadas
 */
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMetricsService metricsService;

    public UserServiceImpl(UserRepository userRepository, UserMetricsService metricsService) {
        this.userRepository = userRepository;
        this.metricsService = metricsService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users.create", description = "Time taken to create a users")
    public User createUser(String name, String email) {
        validateName(name);
        validateEmail(email);

        // Verificar se já existe um usuário com este email (idempotência)
        Optional<User> existingUser = userRepository.findByEmail(email.trim());
        if (existingUser.isPresent()) {
            return existingUser.get(); // Retorna o usuário existente (idempotência)
        }

        User user = User.createNew(name.trim(), email.trim(), "default_password_hash");
        User savedUser = userRepository.save(user);
        metricsService.incrementUserCreated();
        return savedUser;
    }

    @Override
    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findUserById(Long id) {
        validateId(id);
        return userRepository.findById(id);
    }

    @Override
    public List<User> findUsersByName(String name) {
        validateName(name);
        return userRepository.findByName(name.trim());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        validateEmail(email);
        return userRepository.findByEmail(email.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users.update", description = "Time taken to update a users")
    public User updateUser(Long id, String name, String email) {
        validateId(id);
        validateName(name);
        validateEmail(email);

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        // Verificar se os dados são diferentes (idempotência)
        if (existing.getName().equals(name.trim()) && existing.getEmail().equals(email.trim())) {
            return existing; // Retorna o usuário sem alterações (idempotência)
        }

        // Verificar se o email já está em uso por outro usuário
        userRepository.findByEmail(email.trim()).ifPresent(u -> {
            if (!u.getId().equals(id)) {
                throw new ConflictException("Já existe um usuário com este email");
            }
        });
        existing.setName(name.trim());
        existing.setEmail(email.trim());
        User updatedUser = userRepository.update(existing);
        metricsService.incrementUserUpdated();
        return updatedUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users.delete", description = "Time taken to delete a users")
    public boolean deleteUser(Long id) {
        validateId(id);

        // Verificar se o usuário existe antes de tentar deletar (idempotência)
        if (userRepository.findById(id).isEmpty()) {
            return false; // Usuário já não existe (idempotência)
        }

        boolean deleted = userRepository.deleteById(id);
        if (deleted) {
            metricsService.incrementUserDeleted();
        }
        return deleted;
    }

    @Override
    public boolean existsUserById(Long id) {
        validateId(id);
        return userRepository.existsById(id);
    }

    @Override
    public boolean existsUserByEmail(String email) {
        validateEmail(email);
        return userRepository.findByEmail(email.trim()).isPresent();
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public List<User> findUsersByTenant(Long tenantId) {
        if (tenantId == null) {
            throw new ValidationException("ID do tenant não pode ser nulo");
        }
        if (tenantId <= 0) {
            throw new ValidationException("ID do tenant deve ser maior que zero");
        }
        return userRepository.findUsersByTenant(tenantId);
    }

    @Override
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new ValidationException("Query de busca não pode ser vazia");
        }
        return userRepository.search(query.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users.suspend", description = "Time taken to suspend a user")
    public User suspendUser(Long id) {
        validateId(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        if (!user.getActive()) {
            return user; // Usuário já está suspenso (idempotência)
        }

        user.setActive(false);
        User updatedUser = userRepository.update(user);
        metricsService.incrementUserSuspended();
        return updatedUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "users.activate", description = "Time taken to activate a user")
    public User activateUser(Long id) {
        validateId(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));

        if (user.getActive()) {
            return user; // Usuário já está ativo (idempotência)
        }

        user.setActive(true);
        User updatedUser = userRepository.update(user);
        metricsService.incrementUserActivated();
        return updatedUser;
    }

    @Override
    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }

    @Override
    public long countSuspendedUsers() {
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
    public long countUsersByTenant(Long tenantId) {
        if (tenantId == null) {
            throw new ValidationException("ID do tenant não pode ser nulo");
        }
        if (tenantId <= 0) {
            throw new ValidationException("ID do tenant deve ser maior que zero");
        }
        return userRepository.countUsersByTenant(tenantId);
    }

    @Override
    public long countActiveUsersByTenant(Long tenantId) {
        if (tenantId == null) {
            throw new ValidationException("ID do tenant não pode ser nulo");
        }
        if (tenantId <= 0) {
            throw new ValidationException("ID do tenant deve ser maior que zero");
        }
        return userRepository.countActiveUsersByTenant(tenantId);
    }

    @Override
    public long countUsersInPeriod(String startDate, String endDate) {
        if (startDate == null || startDate.trim().isEmpty()) {
            throw new ValidationException("Data de início não pode ser vazia");
        }
        if (endDate == null || endDate.trim().isEmpty()) {
            throw new ValidationException("Data de fim não pode ser vazia");
        }
        return userRepository.countUsersInPeriod(startDate, endDate);
    }

    @Override
    public long countActiveUsersInPeriod(String startDate, String endDate) {
        if (startDate == null || startDate.trim().isEmpty()) {
            throw new ValidationException("Data de início não pode ser vazia");
        }
        if (endDate == null || endDate.trim().isEmpty()) {
            throw new ValidationException("Data de fim não pode ser vazia");
        }
        return userRepository.countActiveUsersInPeriod(startDate, endDate);
    }

    @Override
    public List<User> findTopActiveUsers(int limit) {
        if (limit <= 0) {
            throw new ValidationException("Limite deve ser maior que zero");
        }
        if (limit > 100) {
            throw new ValidationException("Limite não pode ser maior que 100");
        }
        return userRepository.findTopActiveUsers(limit);
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome não pode ser vazio");
        }
        if (name.trim().length() < 2) {
            throw new ValidationException("Nome deve ter pelo menos 2 caracteres");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email não pode ser vazio");
        }
        if (!email.trim().contains("@")) {
            throw new ValidationException("Email deve ter formato válido");
        }
    }

    private void validateId(Long id) {
        if (id == null) {
            throw new ValidationException("ID não pode ser nulo");
        }
        if (id <= 0) {
            throw new ValidationException("ID deve ser maior que zero");
        }
    }
}
