package com.seccreto.service.auth.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;

/**
 * Validador para regras de negócio específicas
 */
@Component
public class BusinessRulesValidator implements ConstraintValidator<BusinessRules, Object> {

    private BusinessRules.BusinessRuleType[] ruleTypes;
    private String[] parameters;

    // Palavras proibidas básicas (pode ser expandido)
    private static final Set<String> PROFANITY_WORDS = Set.of(
        "admin", "root", "system", "test", "null", "undefined", "anonymous",
        "guest", "user", "default", "temp", "temporary", "demo", "sample"
    );

    // Lista de emails/domínios bloqueados (exemplo)
    private static final Set<String> BLACKLISTED_EMAILS = Set.of(
        "spam@example.com", "abuse@example.com", "noreply@example.com"
    );

    @Override
    public void initialize(BusinessRules constraintAnnotation) {
        this.ruleTypes = constraintAnnotation.value();
        this.parameters = constraintAnnotation.parameters();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean isValid = true;

        for (BusinessRules.BusinessRuleType ruleType : ruleTypes) {
            if (!validateRule(ruleType, value, context)) {
                isValid = false;
            }
        }

        return isValid;
    }

    private boolean validateRule(BusinessRules.BusinessRuleType ruleType, Object value, ConstraintValidatorContext context) {
        switch (ruleType) {
            case NO_PROFANITY:
                return validateNoProfanity(value.toString(), context);
            
            case NOT_BLACKLISTED:
                return validateNotBlacklisted(value.toString(), context);
            
            case BUSINESS_HOURS_ONLY:
                return validateBusinessHours(context);
            
            case MINIMUM_AGE:
                return validateMinimumAge(value, context);
            
            case NOT_BOT:
                return validateNotBot(value.toString(), context);
            
            // Outras validações podem ser implementadas conforme necessário
            default:
                return true;
        }
    }

    private boolean validateNoProfanity(String text, ConstraintValidatorContext context) {
        String lowerText = text.toLowerCase();
        
        for (String profanity : PROFANITY_WORDS) {
            if (lowerText.contains(profanity)) {
                context.buildConstraintViolationWithTemplate(
                    String.format("Texto contém palavra não permitida: '%s'", profanity)
                ).addConstraintViolation();
                return false;
            }
        }
        
        return true;
    }

    private boolean validateNotBlacklisted(String email, ConstraintValidatorContext context) {
        if (BLACKLISTED_EMAILS.contains(email.toLowerCase())) {
            context.buildConstraintViolationWithTemplate(
                "Email está na lista de bloqueio"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }

    private boolean validateBusinessHours(ConstraintValidatorContext context) {
        LocalTime now = LocalTime.now();
        LocalTime startTime = LocalTime.of(8, 0); // 08:00
        LocalTime endTime = LocalTime.of(18, 0);  // 18:00
        
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            context.buildConstraintViolationWithTemplate(
                "Registros são permitidos apenas em horário comercial (08:00 - 18:00)"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }

    private boolean validateMinimumAge(Object value, ConstraintValidatorContext context) {
        // Esta validação seria implementada se houvesse um campo de idade
        // Por enquanto, sempre retorna true
        return true;
    }

    private boolean validateNotBot(String userAgent, ConstraintValidatorContext context) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate(
                "User-Agent é obrigatório para verificação anti-bot"
            ).addConstraintViolation();
            return false;
        }
        
        String lowerUserAgent = userAgent.toLowerCase();
        
        // Verificações básicas para detectar bots
        String[] botIndicators = {
            "bot", "crawler", "spider", "scraper", "curl", "wget", "python", "java"
        };
        
        for (String indicator : botIndicators) {
            if (lowerUserAgent.contains(indicator)) {
                context.buildConstraintViolationWithTemplate(
                    "Acesso automatizado detectado"
                ).addConstraintViolation();
                return false;
            }
        }
        
        return true;
    }
}
