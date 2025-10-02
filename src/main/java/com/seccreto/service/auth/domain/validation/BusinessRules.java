package com.seccreto.service.auth.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação para validação de regras de negócio específicas
 * Aplica validações customizadas baseadas em regras de negócio
 */
@Documented
@Constraint(validatedBy = BusinessRulesValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessRules {
    
    String message() default "Violação de regra de negócio";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Tipo de validação de regra de negócio
     */
    BusinessRuleType[] value();
    
    /**
     * Parâmetros adicionais para as regras
     */
    String[] parameters() default {};
    
    enum BusinessRuleType {
        /**
         * Validar se o nome não contém palavras proibidas
         */
        NO_PROFANITY,
        
        /**
         * Validar se o usuário não está em lista de bloqueio
         */
        NOT_BLACKLISTED,
        
        /**
         * Validar limite de usuários por domínio de email
         */
        DOMAIN_USER_LIMIT,
        
        /**
         * Validar se o email não foi usado recentemente por outro usuário
         */
        EMAIL_REUSE_PREVENTION,
        
        /**
         * Validar horário de registro (apenas horário comercial)
         */
        BUSINESS_HOURS_ONLY,
        
        /**
         * Validar países permitidos baseado no IP
         */
        ALLOWED_COUNTRIES,
        
        /**
         * Validar idade mínima (se fornecida)
         */
        MINIMUM_AGE,
        
        /**
         * Validar se não é um bot (verificação básica)
         */
        NOT_BOT
    }
}
