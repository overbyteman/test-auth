package com.seccreto.service.auth.api.dto.auth;

import com.seccreto.service.auth.domain.security.SecureInput;
import com.seccreto.service.auth.domain.validation.BusinessRules;
import com.seccreto.service.auth.domain.validation.EmailDomain;
import com.seccreto.service.auth.domain.validation.PasswordStrength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de registro.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para registro de novo usuário")
public class RegisterRequest {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Schema(description = "Nome completo do usuário", example = "João Silva")
    @SecureInput(message = "Nome contém caracteres não permitidos")
    @BusinessRules(value = {BusinessRules.BusinessRuleType.NO_PROFANITY}, message = "Nome contém palavras não permitidas")
    private String name;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Schema(description = "Email do usuário", example = "joao@empresa.com")
    @SecureInput(message = "Email contém caracteres não permitidos")
    @EmailDomain(blockTemporaryEmails = true, message = "Domínio de email não permitido")
    @BusinessRules(value = {BusinessRules.BusinessRuleType.NOT_BLACKLISTED}, message = "Email não permitido")
    private String email;
    
    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha do usuário", example = "MinhaSenh@123!")
    @PasswordStrength(
        minLength = 8,
        maxLength = 128,
        requireUppercase = true,
        requireLowercase = true,
        requireDigit = true,
        requireSpecialChar = true,
        disallowCommon = true,
        disallowSequences = true,
        disallowRepeats = true,
        message = "Senha não atende aos critérios de segurança"
    )
    @SecureInput(message = "Senha contém caracteres não permitidos")
    private String password;
}
