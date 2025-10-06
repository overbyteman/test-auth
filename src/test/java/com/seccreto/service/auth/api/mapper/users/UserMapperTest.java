package com.seccreto.service.auth.api.mapper.users;

import com.seccreto.service.auth.api.dto.users.UserRequest;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.model.users.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void toResponseShouldMapAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 10, 10, 0);
        LocalDateTime updatedAt = createdAt.plusDays(1);

        User user = User.builder()
                .id(id)
                .name("John Doe")
                .email("john@example.com")
                .passwordHash("hash")
                .isActive(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        UserResponse response = UserMapper.toResponse(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toResponseShouldHandleNullUser() {
        assertThat(UserMapper.toResponse(null)).isNull();
    }

    @Test
    void toResponseListShouldMapAllItems() {
        User user1 = User.createNew("John Doe", "john@example.com", "hash");
        user1.setId(UUID.randomUUID());
        user1.setCreatedAt(LocalDateTime.of(2024, 1, 1, 8, 0));
        user1.setUpdatedAt(LocalDateTime.of(2024, 1, 2, 9, 0));

        User user2 = User.createNew("Jane Smith", "jane@example.com", "hash");
        user2.setId(UUID.randomUUID());
        user2.setCreatedAt(LocalDateTime.of(2024, 2, 1, 8, 0));
        user2.setUpdatedAt(LocalDateTime.of(2024, 2, 2, 9, 0));

        List<UserResponse> responses = UserMapper.toResponseList(List.of(user1, user2));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("John Doe");
        assertThat(responses.get(1).getName()).isEqualTo("Jane Smith");
    }

    @Test
    void toEntityShouldMapFieldsFromRequest() {
        UserRequest request = UserRequest.builder()
                .name("Ana")
                .email("ana@example.com")
                .password("SenhaForte123!")
                .build();

        User entity = UserMapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("Ana");
        assertThat(entity.getEmail()).isEqualTo("ana@example.com");
        assertThat(entity.getId()).isNull();
    }

    @Test
    void updateEntityShouldApplyRequestData() {
        User user = User.createNew("Original", "original@example.com", "hash");
        user.setId(UUID.randomUUID());

        UserRequest request = UserRequest.builder()
                .name("Atualizado")
                .email("atualizado@example.com")
                .password("SenhaForte123!")
                .build();

        UserMapper.updateEntity(user, request);

        assertThat(user.getName()).isEqualTo("Atualizado");
        assertThat(user.getEmail()).isEqualTo("atualizado@example.com");
    }
}