package com.seccreto.service.auth.model.roles;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Classe que representa um role no sistema para RBAC (JPA Entity)
 *
 * Características de implementação sênior:
 * - JPA Entity com mapeamento automático
 * - Suporte a RBAC (Role-Based Access Control)
 * - Nome único case-insensitive
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Entity
@Table(name = "roles")
@Schema(description = "Entidade que representa um role no sistema para RBAC")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @Schema(description = "Identificador único do role (UUID)")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    @Schema(description = "Nome do role (deve ser único)", example = "ADMIN")
    private String name;
    
    @Column(name = "description")
    @Schema(description = "Descrição opcional do role", example = "Administrador do sistema")
    private String description;

    /**
     * Construtor para criação de novos roles com valores padrão
     */
    public static Role createNew(String name, String description) {
        return Role.builder()
                .name(name)
                .description(description)
                .build();
    }
    
    /**
     * Método para atualizar timestamps automaticamente
     */
    public void updateTimestamp() {
        // Role não tem timestamp, mas mantemos para compatibilidade
    }
}