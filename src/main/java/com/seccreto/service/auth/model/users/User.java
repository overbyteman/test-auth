package com.seccreto.service.auth.model.users;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seccreto.service.auth.model.users_tenants_roles.UsersTenantsRoles;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Classe que representa um usuário no sistema (JPA Entity)
 * 
 * Características de implementação sênior:
 * - JPA Entity com mapeamento automático
 * - Timestamps automáticos com Hibernate
 * - Suporte a UUID como chave primária
 * - Multi-tenancy: usuário pode ter múltiplos roles em múltiplos tenants
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Entity
@Table(name = "users")
@Schema(description = "Entidade que representa um usuário no sistema com suporte a multi-tenancy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passwordHash", "emailVerificationToken", "tenantRoles"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @Schema(description = "Identificador único do usuário (UUID)")
    @EqualsAndHashCode.Include
    private UUID id;
    
    @Column(name = "name", nullable = false)
    @Schema(description = "Nome completo do usuário", example = "João Silva")
    private String name;
    
    @Column(name = "email", nullable = false, unique = true)
    @Schema(description = "Email do usuário", example = "joao@email.com")
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    @Schema(description = "Hash da senha do usuário")
    @JsonIgnore
    private String passwordHash;
    
    @Column(name = "is_active", nullable = false)
    @Schema(description = "Indica se o usuário está ativo", example = "false")
    private Boolean isActive;
    
    @Column(name = "email_verification_token")
    @Schema(description = "Token de verificação de email")
    @JsonIgnore
    private String emailVerificationToken;
    
    @Column(name = "email_verified_at")
    @Schema(description = "Data de verificação do email")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime emailVerifiedAt;
    
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Schema(description = "Relacionamentos do usuário com tenants e roles")
    private Set<UsersTenantsRoles> tenantRoles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Data e hora de criação do usuário")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Data e hora da última atualização do usuário")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Construtor para criação de novos usuários com valores padrão
     * Timestamps são gerenciados automaticamente pelo Hibernate
     */
    public static User createNew(String name, String email, String passwordHash) {
        return User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordHash)
                .isActive(true)
                .tenantRoles(new HashSet<>())
                .build();
    }
    
    /**
     * Adiciona um relacionamento tenant-role ao usuário
     */
    public void addTenantRole(UsersTenantsRoles tenantRole) {
        if (this.tenantRoles == null) {
            this.tenantRoles = new HashSet<>();
        }
        this.tenantRoles.add(tenantRole);
    }

    /**
     * Remove um relacionamento tenant-role do usuário
     */
    public void removeTenantRole(UsersTenantsRoles tenantRole) {
        if (this.tenantRoles != null) {
            this.tenantRoles.remove(tenantRole);
        }
    }

    /**
     * Método para atualizar timestamps automaticamente
     * Hibernate gerencia automaticamente via @UpdateTimestamp
     */
    public void updateTimestamp() {
        // Hibernate atualiza automaticamente o updatedAt
    }
    
    /**
     * Verifica se o usuário está ativo
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }
}
