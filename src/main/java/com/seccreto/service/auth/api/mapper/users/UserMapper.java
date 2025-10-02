package com.seccreto.service.auth.api.mapper.users;

import com.seccreto.service.auth.api.dto.users.UserRequest;
import com.seccreto.service.auth.api.dto.users.UserResponse;
import com.seccreto.service.auth.model.users.User;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper para conversão entre DTOs e entidades de User.
 * Implementa o padrão de mapeamento estático para performance.
 */
public final class UserMapper {
    private UserMapper() {}

    /**
     * Converte UserRequest para entidade User.
     */
    public static User toEntity(UserRequest request) {
        if (request == null) {
            return null;
        }
        
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
    }

    /**
     * Converte entidade User para UserResponse.
     */
    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * Converte lista de entidades User para lista de UserResponse.
     */
    public static List<UserResponse> toResponseList(List<User> users) {
        if (users == null) {
            return null;
        }
        
        return users.stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza entidade User com dados do UserRequest.
     */
    public static void updateEntity(User user, UserRequest request) {
        if (user == null || request == null) {
            return;
        }
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
    }
}

