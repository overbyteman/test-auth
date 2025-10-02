package com.seccreto.service.auth.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validador para força de senha
 */
public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, String> {

    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecialChar;
    private int minLength;
    private int maxLength;
    private boolean disallowCommon;
    private boolean disallowSequences;
    private boolean disallowRepeats;

    // Senhas comuns mais utilizadas
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "123456", "123456789", "12345678", "12345", "1234567", "1234567890",
        "qwerty", "abc123", "111111", "123123", "admin", "letmein", "welcome", "monkey",
        "password123", "123qwe", "qwerty123", "password1", "admin123", "root", "toor",
        "pass", "test", "guest", "user", "login", "senha", "senha123", "123mudar"
    );

    // Padrões para caracteres especiais
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");

    @Override
    public void initialize(PasswordStrength constraintAnnotation) {
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecialChar = constraintAnnotation.requireSpecialChar();
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
        this.disallowCommon = constraintAnnotation.disallowCommon();
        this.disallowSequences = constraintAnnotation.disallowSequences();
        this.disallowRepeats = constraintAnnotation.disallowRepeats();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true; // Handled by @NotNull
        }

        context.disableDefaultConstraintViolation();
        boolean isValid = true;

        // Verificar comprimento
        if (password.length() < minLength) {
            context.buildConstraintViolationWithTemplate(
                String.format("Senha deve ter pelo menos %d caracteres", minLength)
            ).addConstraintViolation();
            isValid = false;
        }

        if (password.length() > maxLength) {
            context.buildConstraintViolationWithTemplate(
                String.format("Senha deve ter no máximo %d caracteres", maxLength)
            ).addConstraintViolation();
            isValid = false;
        }

        // Verificar maiúscula
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate(
                "Senha deve conter pelo menos uma letra maiúscula"
            ).addConstraintViolation();
            isValid = false;
        }

        // Verificar minúscula
        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate(
                "Senha deve conter pelo menos uma letra minúscula"
            ).addConstraintViolation();
            isValid = false;
        }

        // Verificar dígito
        if (requireDigit && !DIGIT_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate(
                "Senha deve conter pelo menos um número"
            ).addConstraintViolation();
            isValid = false;
        }

        // Verificar caractere especial
        if (requireSpecialChar && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            context.buildConstraintViolationWithTemplate(
                "Senha deve conter pelo menos um caractere especial (!@#$%^&*()_+-=[]{}|;':\"\\,.<>?/)"
            ).addConstraintViolation();
            isValid = false;
        }

        // Verificar senhas comuns
        if (disallowCommon && COMMON_PASSWORDS.contains(password.toLowerCase())) {
            context.buildConstraintViolationWithTemplate(
                "Senha muito comum. Escolha uma senha mais segura"
            ).addConstraintViolation();
            isValid = false;
        }

        // Verificar sequências
        if (disallowSequences && containsSequence(password)) {
            context.buildConstraintViolationWithTemplate(
                "Senha não pode conter sequências (123, abc, etc.)"
            ).addConstraintViolation();
            isValid = false;
        }

        // Verificar repetições
        if (disallowRepeats && containsExcessiveRepeats(password)) {
            context.buildConstraintViolationWithTemplate(
                "Senha não pode conter muitos caracteres repetidos consecutivos"
            ).addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }

    /**
     * Verifica se a senha contém sequências numéricas ou alfabéticas
     */
    private boolean containsSequence(String password) {
        String lower = password.toLowerCase();
        
        // Verificar sequências numéricas (123, 456, etc.)
        for (int i = 0; i < lower.length() - 2; i++) {
            char c1 = lower.charAt(i);
            char c2 = lower.charAt(i + 1);
            char c3 = lower.charAt(i + 2);
            
            if (Character.isDigit(c1) && Character.isDigit(c2) && Character.isDigit(c3)) {
                if ((c2 == c1 + 1) && (c3 == c2 + 1)) {
                    return true; // Sequência crescente
                }
                if ((c2 == c1 - 1) && (c3 == c2 - 1)) {
                    return true; // Sequência decrescente
                }
            }
            
            // Verificar sequências alfabéticas (abc, def, etc.)
            if (Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3)) {
                if ((c2 == c1 + 1) && (c3 == c2 + 1)) {
                    return true; // Sequência crescente
                }
                if ((c2 == c1 - 1) && (c3 == c2 - 1)) {
                    return true; // Sequência decrescente
                }
            }
        }
        
        return false;
    }

    /**
     * Verifica se a senha contém muitos caracteres repetidos consecutivos
     */
    private boolean containsExcessiveRepeats(String password) {
        int maxRepeats = 2; // Máximo de 2 caracteres iguais consecutivos
        
        for (int i = 0; i < password.length() - maxRepeats; i++) {
            char current = password.charAt(i);
            int repeatCount = 1;
            
            for (int j = i + 1; j < password.length() && password.charAt(j) == current; j++) {
                repeatCount++;
                if (repeatCount > maxRepeats) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
