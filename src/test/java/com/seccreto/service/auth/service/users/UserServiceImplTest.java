package com.seccreto.service.auth.service.users;

import com.seccreto.service.auth.api.dto.common.Pagination;
import com.seccreto.service.auth.api.dto.common.SearchQuery;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.repository.users.UserRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import com.seccreto.service.auth.service.usage.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMetricsService metricsService;

    @Mock
    private UsageService usageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = createUser("John Doe", "john@example.com");
    }

    @Test
    void createUserShouldEncodePasswordAndReturnSavedUser() {
        String name = " John Doe ";
    String email = "john@example.com";
        String password = "password123";
        String hashed = "hashedPassword123";

    when(userRepository.findByEmail(email.trim())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(hashed);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toPersist = invocation.getArgument(0);
            toPersist.setId(UUID.randomUUID());
            return toPersist;
        });

        User result = userService.createUser(name, email, password);

        assertThat(result.getName()).isEqualTo(name.trim());
        assertThat(result.getEmail()).isEqualTo(email.trim());
        assertThat(result.getPasswordHash()).isEqualTo(hashed);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(hashed);

        verify(metricsService).incrementUserCreated();
    }

    @Test
    void createUserShouldReturnExistingWhenEmailFound() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        User result = userService.createUser(user.getName(), user.getEmail(), "ignored");

        assertThat(result).isSameAs(user);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
        verify(metricsService, never()).incrementUserCreated();
    }

    @Test
    void createUserShouldValidateName() {
        assertThatThrownBy(() -> userService.createUser(" ", user.getEmail(), "pass"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Nome do usuário é obrigatório");

        assertThatThrownBy(() -> userService.createUser("A", user.getEmail(), "pass"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Nome do usuário deve ter pelo menos 2 caracteres");
    }

    @Test
    void createUserShouldValidateEmail() {
        assertThatThrownBy(() -> userService.createUser(user.getName(), " ", "pass"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Email do usuário é obrigatório");

        assertThatThrownBy(() -> userService.createUser(user.getName(), "invalid", "pass"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Email deve ter um formato válido");
    }

    @Test
    void listAllUsersShouldReturnRepositoryResult() {
        List<User> users = List.of(user, createUser("Jane Smith", "jane@example.com"));
        when(userRepository.findAll()).thenReturn(users);

        assertThat(userService.listAllUsers()).isEqualTo(users);
    }

    @Test
    void findUserByIdShouldValidateId() {
        assertThatThrownBy(() -> userService.findUserById(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("ID do usuário não pode ser nulo");
    }

    @Test
    void updateUserShouldApplyChanges() {
        UUID userId = user.getId();
        String newName = "John Updated";
        String newEmail = "updated@example.com";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateUser(userId, newName, newEmail);

        assertThat(result.getName()).isEqualTo(newName);
        assertThat(result.getEmail()).isEqualTo(newEmail);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserShouldThrowWhenEmailBelongsToAnotherUser() {
        UUID userId = user.getId();
        User another = createUser("Jane", "existing@example.com");
        another.setId(UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(another.getEmail())).thenReturn(Optional.of(another));

        assertThatThrownBy(() -> userService.updateUser(userId, "New", another.getEmail()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email já está em uso por outro usuário");
    }

    @Test
    void updateUserShouldThrowWhenUserMissing() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(userId, "New", "new@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com ID: " + userId);
    }

    @Test
    void deleteUserShouldRemoveAndIncrementMetrics() {
        UUID userId = user.getId();
        when(userRepository.existsById(userId)).thenReturn(true);

        boolean result = userService.deleteUser(userId);

        assertThat(result).isTrue();
        verify(userRepository).deleteById(userId);
        verify(metricsService).incrementUserDeleted();
    }

    @Test
    void deleteUserShouldThrowWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com ID: " + userId);
    }

    @Test
    void activateUserShouldSetActive() {
        UUID userId = user.getId();
        user.setIsActive(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.activateUser(userId);

        assertThat(result.isActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUserShouldClearActive() {
        UUID userId = user.getId();
        user.setIsActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.deactivateUser(userId);

        assertThat(result.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void searchUsersShouldReturnPagination() {
        SearchQuery searchQuery = new SearchQuery(1, 10, "john", "name", "ASC");
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.search("john", PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"))))
                .thenReturn(page);

    Pagination<?> result = userService.searchUsers(searchQuery);

    assertThat(result.currentPage()).isEqualTo(1);
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0)).extracting("name").isEqualTo("John Doe");
    }

    @Test
    void countUsersShouldDelegate() {
        when(userRepository.count()).thenReturn(42L);
        assertThat(userService.countUsers()).isEqualTo(42L);
    }

    @Test
    void countActiveUsersShouldDelegate() {
        when(userRepository.countActiveUsers()).thenReturn(30L);
        assertThat(userService.countActiveUsers()).isEqualTo(30L);
    }

    @Test
    void countInactiveUsersShouldDelegate() {
        when(userRepository.countSuspendedUsers()).thenReturn(12L);
        assertThat(userService.countInactiveUsers()).isEqualTo(12L);
    }

    @Test
    void countUsersCreatedTodayShouldDelegate() {
        when(userRepository.countUsersCreatedToday()).thenReturn(5L);
        assertThat(userService.countUsersCreatedToday()).isEqualTo(5L);
    }

    @Test
    void countUsersByTenantShouldDelegate() {
        UUID tenantId = UUID.randomUUID();
        when(userRepository.countUsersByTenant(tenantId)).thenReturn(9L);

        assertThat(userService.countUsersByTenant(tenantId)).isEqualTo(9L);
    }

    @Test
    void recordUserLoginShouldDelegateToUsageService() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        userService.recordUserLogin(userId, tenantId);

    verify(usageService).recordUserLogin(eq(userId), eq(tenantId), any(LocalDate.class));
    }

    @Test
    void recordUserActionShouldDelegateToUsageService() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        userService.recordUserAction(userId, tenantId);

    verify(usageService).recordUserAction(eq(userId), eq(tenantId), any(LocalDate.class), any(LocalDateTime.class));
    }

    private User createUser(String name, String email) {
        User newUser = User.createNew(name, email, "hashed");
        newUser.setId(UUID.randomUUID());
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        return newUser;
    }
}
