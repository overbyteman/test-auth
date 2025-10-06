package com.seccreto.service.auth.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validador para domínios de email
 */
public class EmailDomainValidator implements ConstraintValidator<EmailDomain, String> {

    private Set<String> allowedDomains;
    private Set<String> blockedDomains;
    private boolean blockTemporaryEmails;
    private boolean corporateOnly;

    // Domínios temporários conhecidos
    private static final Set<String> TEMPORARY_EMAIL_DOMAINS = Set.of(
        "10minutemail.com", "guerrillamail.com", "mailinator.com", "tempmail.org",
        "throwaway.email", "temp-mail.org", "yopmail.com", "maildrop.cc",
        "sharklasers.com", "getnada.com", "tempail.com", "dispostable.com",
        "fakeinbox.com", "spamgourmet.com", "trashmail.com", "emailondeck.com",
        "mohmal.com", "mytrashmail.com", "tempinbox.com", "tempr.email",
        "disposablemail.com", "burnermail.io", "guerrillamailblock.com"
    );

    // Domínios de email pessoal populares
    private static final Set<String> PERSONAL_EMAIL_DOMAINS = Set.of(
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "live.com",
        "msn.com", "aol.com", "icloud.com", "me.com", "mac.com",
        "protonmail.com", "tutanota.com", "zoho.com", "mail.com",
        "gmx.com", "yandex.com", "mail.ru", "qq.com", "163.com",
        "sina.com", "sohu.com", "126.com", "yeah.net", "foxmail.com"
    );

    @Override
    public void initialize(EmailDomain constraintAnnotation) {
        // Normalize domains to lower-case to ensure case-insensitive checks
        this.allowedDomains = Arrays.stream(constraintAnnotation.allowedDomains())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        this.blockedDomains = Arrays.stream(constraintAnnotation.blockedDomains())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        this.blockTemporaryEmails = constraintAnnotation.blockTemporaryEmails();
        this.corporateOnly = constraintAnnotation.corporateOnly();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.trim().isEmpty()) {
            return true; // Handled by @NotNull/@NotEmpty
        }

        String domain = extractDomain(email);
        if (domain == null) {
            return false; // Email inválido
        }

        domain = domain.toLowerCase();
        context.disableDefaultConstraintViolation();

        // Se há whitelist, verificar se o domínio está na lista
        if (!allowedDomains.isEmpty()) {
            if (!allowedDomains.contains(domain)) {
                context.buildConstraintViolationWithTemplate(
                    String.format("Domínio '%s' não está na lista de domínios permitidos", domain)
                ).addConstraintViolation();
                return false;
            }
        }

        // Verificar blacklist
        if (blockedDomains.contains(domain)) {
            context.buildConstraintViolationWithTemplate(
                String.format("Domínio '%s' não é permitido", domain)
            ).addConstraintViolation();
            return false;
        }

        // Verificar domínios temporários
        if (blockTemporaryEmails && TEMPORARY_EMAIL_DOMAINS.contains(domain)) {
            context.buildConstraintViolationWithTemplate(
                "Emails temporários não são permitidos"
            ).addConstraintViolation();
            return false;
        }

        // Verificar se é apenas corporativo
        if (corporateOnly && PERSONAL_EMAIL_DOMAINS.contains(domain)) {
            context.buildConstraintViolationWithTemplate(
                "Apenas emails corporativos são permitidos"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    /**
     * Extrai o domínio do email
     */
    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1);
    }
}
