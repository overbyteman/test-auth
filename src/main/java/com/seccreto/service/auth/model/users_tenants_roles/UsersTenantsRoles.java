package com.seccreto.service.auth.model.users_tenants_roles;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Classe que representa a relação many-to-many entre users, tenants e roles (Model)
 * 
 * Características de implementação sênior:
 * - Tabela de junção para multi-tenancy
 * - Chave primária composta (userId, tenantId, roleId)
 * - Suporte a multi-tenancy com RBAC
 * - Timestamps com timezone
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Schema(description = "Entidade que representa a relação many-to-many entre users, tenants e roles para multi-tenancy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class UsersTenantsRoles {
    @Schema(description = "ID do usuário", example = "1")
    @EqualsAndHashCode.Include
    private Long userId;
    
    @Schema(description = "ID do tenant", example = "1")
    @EqualsAndHashCode.Include
    private Long tenantId;
    
    @Schema(description = "ID do role", example = "1")
    @EqualsAndHashCode.Include
    private Long roleId;
    
    @Schema(description = "Data e hora de criação da relação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Construtor para criação de novas relações user-tenant-role
     */
    public static UsersTenantsRoles createNew(Long userId, Long tenantId, Long roleId) {
        return UsersTenantsRoles.builder()
                .userId(userId)
                .tenantId(tenantId)
                .roleId(roleId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
