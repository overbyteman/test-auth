package com.seccreto.service.auth.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação para validação de força de senha
 * Verifica se a senha atende aos critérios de segurança
 */
@Documented
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {
    
    String message() default "Senha não atende aos critérios de segurança";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Requer pelo menos uma letra maiúscula
     */
    boolean requireUppercase() default true;
    
    /**
     * Requer pelo menos uma letra minúscula
     */
    boolean requireLowercase() default true;
    
    /**
     * Requer pelo menos um número
     */
    boolean requireDigit() default true;
    
    /**
     * Requer pelo menos um caractere especial
     */
    boolean requireSpecialChar() default true;
    
    /**
     * Comprimento mínimo da senha
     */
    int minLength() default 8;
    
    /**
     * Comprimento máximo da senha
     */
    int maxLength() default 128;
    
    /**
     * Não permitir senhas comuns
     */
    boolean disallowCommon() default true;
    
    /**
     * Não permitir sequências (123, abc, etc.)
     */
    boolean disallowSequences() default true;
    
    /**
     * Não permitir repetições excessivas (aaa, 111, etc.)
     */
    boolean disallowRepeats() default true;
}
