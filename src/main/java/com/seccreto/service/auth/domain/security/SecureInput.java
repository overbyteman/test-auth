package com.seccreto.service.auth.domain.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação para validação de entrada segura
 * Aplica validação contra SQL injection, XSS e outros ataques
 */
@Documented
@Constraint(validatedBy = SecureInputValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SecureInput {
    
    String message() default "Input contains potentially dangerous content";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Se deve sanitizar automaticamente o input
     */
    boolean sanitize() default false;
    
    /**
     * Se deve permitir HTML básico (apenas tags seguras)
     */
    boolean allowBasicHtml() default false;
}
