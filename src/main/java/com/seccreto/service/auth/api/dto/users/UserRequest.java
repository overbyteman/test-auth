package com.seccreto.service.auth.api.dto.users;

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

@Schema(description = "Payload de criação/atualização de usuário")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @Schema(description = "Nome completo do usuário", example = "João Silva")
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @SecureInput(message = "Nome contém caracteres não permitidos")
    @BusinessRules(value = {BusinessRules.BusinessRuleType.NO_PROFANITY}, message = "Nome contém palavras não permitidas")
    private String name;

    @Schema(description = "Email do usuário", example = "joao@empresa.com")
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    @SecureInput(message = "Email contém caracteres não permitidos")
    @EmailDomain(blockTemporaryEmails = true, message = "Domínio de email não permitido")
    @BusinessRules(value = {BusinessRules.BusinessRuleType.NOT_BLACKLISTED}, message = "Email não permitido")
    private String email;

    @Schema(description = "Senha do usuário", example = "MinhaSenh@123!")
    @NotBlank(message = "Senha é obrigatória")
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

