package com.seccreto.service.auth.model.policies;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum que representa os efeitos de uma política ABAC (Attribute-Based Access Control)
 * 
 * Características de implementação sênior:
 * - Suporte a ABAC (Attribute-Based Access Control)
 * - Efeitos de política (allow/deny)
 * - Documentação completa com Swagger
 * - Validações de negócio
 */
@Schema(description = "Enum que representa os efeitos de uma política ABAC")
public enum PolicyEffect {
    @Schema(description = "Política que permite acesso")
    ALLOW,
    
    @Schema(description = "Política que nega acesso")
    DENY
}
