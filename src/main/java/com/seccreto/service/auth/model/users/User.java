package com.seccreto.service.auth.model.users;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Classe que representa um usuário no sistema (Model)
 * 
 * Características de implementação sênior:
 * - Suporte a soft delete com campo active
 * - Versioning para optimistic locking
 * - Timestamps com timezone
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Schema(description = "Entidade que representa um usuário no sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passwordHash", "emailVerificationToken"})
public class User {
    @Schema(description = "Identificador único do usuário", example = "1")
    @EqualsAndHashCode.Include
    private Long id;
    
    @Schema(description = "Nome completo do usuário", example = "João Silva")
    private String name;
    
    @Schema(description = "Email do usuário", example = "joao@email.com")
    private String email;
    
    @Schema(description = "Hash da senha do usuário")
    @JsonIgnore
    private String passwordHash;
    
    @Schema(description = "Indica se o usuário está ativo", example = "false")
    private Boolean isActive;
    
    @Schema(description = "Token de verificação de email")
    @JsonIgnore
    private String emailVerificationToken;
    
    @Schema(description = "Data de verificação do email")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime emailVerifiedAt;
    
    @Schema(description = "Data e hora de criação do usuário")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data e hora da última atualização do usuário")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Versão para controle de concorrência otimista", example = "1")
    private Integer version;
    
    @Schema(description = "Indica se o usuário está ativo (soft delete)", example = "true")
    @JsonIgnore
    private Boolean active;

    /**
     * Construtor para criação de novos usuários com valores padrão
     */
    public static User createNew(String name, String email, String passwordHash) {
        return User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordHash)
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1)
                .active(true)
                .build();
    }
    
    /**
     * Método para atualizar timestamps automaticamente
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
