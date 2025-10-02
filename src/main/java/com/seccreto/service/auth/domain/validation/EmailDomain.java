package com.seccreto.service.auth.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação para validação de domínios de email
 * Permite controlar quais domínios são aceitos ou rejeitados
 */
@Documented
@Constraint(validatedBy = EmailDomainValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailDomain {
    
    String message() default "Domínio de email não permitido";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Lista de domínios permitidos (whitelist)
     * Se vazio, todos os domínios são permitidos (exceto os bloqueados)
     */
    String[] allowedDomains() default {};
    
    /**
     * Lista de domínios bloqueados (blacklist)
     */
    String[] blockedDomains() default {
        "10minutemail.com", "guerrillamail.com", "mailinator.com", 
        "tempmail.org", "throwaway.email", "temp-mail.org",
        "yopmail.com", "maildrop.cc", "sharklasers.com"
    };
    
    /**
     * Bloquear domínios temporários conhecidos
     */
    boolean blockTemporaryEmails() default true;
    
    /**
     * Permitir apenas domínios corporativos (bloquear gmail, yahoo, etc.)
     */
    boolean corporateOnly() default false;
}
