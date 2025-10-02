package com.seccreto.service.auth.domain.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador para a anotação @SecureInput
 */
public class SecureInputValidator implements ConstraintValidator<SecureInput, String> {

    private boolean sanitize;
    private boolean allowBasicHtml;

    @Override
    public void initialize(SecureInput constraintAnnotation) {
        this.sanitize = constraintAnnotation.sanitize();
        this.allowBasicHtml = constraintAnnotation.allowBasicHtml();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Null/empty values are handled by @NotNull/@NotEmpty
        }

        try {
            // Se deve sanitizar, aplica sanitização
            if (sanitize) {
                value = InputSanitizer.sanitize(value);
            }

            // Se permite HTML básico, aplica validação mais leniente
            if (allowBasicHtml) {
                // Apenas verifica SQL injection e JavaScript injection
                return !InputSanitizer.containsSqlInjection(value) && 
                       !InputSanitizer.containsInjection(value);
            }

            // Validação completa
            return InputSanitizer.isSafe(value);
            
        } catch (Exception e) {
            // Se houver erro na validação, considera inválido
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Security validation failed: " + e.getMessage()
            ).addConstraintViolation();
            return false;
        }
    }
}
