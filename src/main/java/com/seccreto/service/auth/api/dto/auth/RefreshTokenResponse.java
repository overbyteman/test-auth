package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Response do refresh de token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    @Schema(description = "Novo access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Novo refresh token (opcional)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Tipo do token", example = "Bearer")
    private String tokenType;

    @Schema(description = "Tempo de expiração em segundos", example = "3600")
    private Long expiresIn;

    @Schema(description = "ID do usuário")
    private UUID userId;

    @Schema(description = "Timestamp do refresh")
    private LocalDateTime refreshedAt;

    @Schema(description = "ID do tenant ativo")
    private UUID tenantId;

    @Schema(description = "Nome do tenant ativo")
    private String tenantName;

    @Schema(description = "Roles do usuário com suas permissões")
    private List<RolePermissionsResponse> roles;
}
