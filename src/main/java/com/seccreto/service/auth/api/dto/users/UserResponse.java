package com.seccreto.service.auth.api.dto.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Resposta de usuário")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    @Schema(description = "ID do usuário", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    @Schema(description = "Nome do usuário")
    private String name;
    @Schema(description = "Email do usuário")
    private String email;
    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;
    @Schema(description = "Data de atualização")
    private LocalDateTime updatedAt;
}

