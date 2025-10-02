package com.seccreto.service.auth.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para resposta de perfil completo do usuário.
 * Baseado no formato real esperado pelo sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta de perfil completo do usuário")
public class UserProfileResponse {
    
    @Schema(description = "Indica se o token é válido", example = "true")
    private Boolean valid;
    
    @Schema(description = "ID do usuário", example = "b492ccf6-f901-4505-9d4c-588ad71e3a81")
    private UUID userId;
    
    @Schema(description = "ID da sessão atual", example = "958dd7fd-6a78-4f7e-81ab-63cc87fab906")
    private UUID sessionId;
    
    @Schema(description = "ID do tenant atual", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID tenantId;
    
    @Schema(description = "Informações do usuário")
    private UserInfo userInfo;
    
    @Schema(description = "Lista de permissões do usuário", example = "[\"create:users\", \"read:users\"]")
    private List<String> permissions;
    
    @Schema(description = "Lista de roles do usuário", example = "[\"ADMIN\", \"USER\"]")
    private List<String> roles;
    
    @Schema(description = "Lista de políticas aplicáveis", example = "[\"admin-policy\", \"user-policy\"]")
    private List<String> policies;
    
    @Schema(description = "Data de expiração do token")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Motivo de invalidação (se aplicável)")
    private String reason;
    
    /**
     * Informações do usuário
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Informações detalhadas do usuário")
    public static class UserInfo {
        @Schema(description = "Nome do usuário", example = "Felipe Duque")
        private String name;
        
        @Schema(description = "Email do usuário", example = "felipe@duque.dev")
        private String email;
        
        @Schema(description = "Status ativo do usuário", example = "true")
        private Boolean active;
        
        public static UserInfo of(String name, String email, Boolean active) {
            UserInfo userInfo = new UserInfo();
            userInfo.name = name;
            userInfo.email = email;
            userInfo.active = active;
            return userInfo;
        }
    }
}
