package com.seccreto.service.auth.domain.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailDomainValidatorTest {

    private EmailDomainValidator validator;
    private EmailDomain constraintAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new EmailDomainValidator();
        constraintAnnotation = mock(EmailDomain.class);
        
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void shouldInitializeWithDefaultSettings() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(false);

        // When
        validator.initialize(constraintAnnotation);

        // Then
        verify(constraintAnnotation).allowedDomains();
        verify(constraintAnnotation).blockedDomains();
        verify(constraintAnnotation).blockTemporaryEmails();
        verify(constraintAnnotation).corporateOnly();
    }

    @Test
    void shouldReturnTrueForNullEmail() {
        // Given
        setupDefaultConstraints();

        // When
        boolean result = validator.isValid(null, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldReturnTrueForEmptyEmail() {
        // Given
        setupDefaultConstraints();

        // When
        boolean result = validator.isValid("", context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldAcceptValidEmailWithDefaultSettings() {
        // Given
        setupDefaultConstraints();
        String validEmail = "user@example.com";

        // When
        boolean result = validator.isValid(validEmail, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldRejectTemporaryEmailWhenBlockTemporaryEmailsIsTrue() {
        // Given
        setupDefaultConstraints();
        String temporaryEmail = "user@10minutemail.com";

        // When
        boolean result = validator.isValid(temporaryEmail, context);

        // Then
    assertThat(result).isFalse();
    verify(context).buildConstraintViolationWithTemplate("Emails temporários não são permitidos");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldAcceptTemporaryEmailWhenBlockTemporaryEmailsIsFalse() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(false);
        when(constraintAnnotation.corporateOnly()).thenReturn(false);
        validator.initialize(constraintAnnotation);

        String temporaryEmail = "user@10minutemail.com";

        // When
        boolean result = validator.isValid(temporaryEmail, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldRejectPersonalEmailWhenCorporateOnlyIsTrue() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(true);
        validator.initialize(constraintAnnotation);

        String personalEmail = "user@gmail.com";

        // When
        boolean result = validator.isValid(personalEmail, context);

        // Then
        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Apenas emails corporativos são permitidos");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldAcceptCorporateEmailWhenCorporateOnlyIsTrue() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(true);
        validator.initialize(constraintAnnotation);

        String corporateEmail = "user@company.com";

        // When
        boolean result = validator.isValid(corporateEmail, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldAcceptEmailFromAllowedDomains() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[]{"company.com", "example.org"});
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(false);
        validator.initialize(constraintAnnotation);

        String allowedEmail = "user@company.com";

        // When
        boolean result = validator.isValid(allowedEmail, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldRejectEmailFromNonAllowedDomains() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[]{"company.com", "example.org"});
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(false);
        validator.initialize(constraintAnnotation);

        String nonAllowedEmail = "user@other.com";

        // When
        boolean result = validator.isValid(nonAllowedEmail, context);

        // Then
    assertThat(result).isFalse();
    verify(context).buildConstraintViolationWithTemplate("Domínio 'other.com' não está na lista de domínios permitidos");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldRejectEmailFromBlockedDomains() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[]{"blocked.com", "spam.org"});
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(false);
        validator.initialize(constraintAnnotation);

        String blockedEmail = "user@blocked.com";

        // When
        boolean result = validator.isValid(blockedEmail, context);

        // Then
    assertThat(result).isFalse();
    verify(context).buildConstraintViolationWithTemplate("Domínio 'blocked.com' não é permitido");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldAcceptEmailFromNonBlockedDomains() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[]{"blocked.com", "spam.org"});
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(false);
        validator.initialize(constraintAnnotation);

        String allowedEmail = "user@allowed.com";

        // When
        boolean result = validator.isValid(allowedEmail, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldHandleCaseInsensitiveDomainCheck() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[]{"COMPANY.COM"});
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(false);
        validator.initialize(constraintAnnotation);

        String emailWithLowercaseDomain = "user@company.com";

        // When
        boolean result = validator.isValid(emailWithLowercaseDomain, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldHandleMultipleBlockedDomains() {
        // Given
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[]{"blocked1.com", "blocked2.org", "blocked3.net"});
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(false);
        validator.initialize(constraintAnnotation);

        String blockedEmail = "user@blocked2.org";

        // When
        boolean result = validator.isValid(blockedEmail, context);

        // Then
    assertThat(result).isFalse();
    verify(context).buildConstraintViolationWithTemplate("Domínio 'blocked2.org' não é permitido");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldHandleInvalidEmailFormat() {
        // Given
        setupDefaultConstraints();
        String invalidEmail = "not-an-email";

        // When
        boolean result = validator.isValid(invalidEmail, context);

        // Then
        // Implementation treats invalid email format as invalid (returns false)
        assertThat(result).isFalse();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    private void setupDefaultConstraints() {
        when(constraintAnnotation.allowedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockedDomains()).thenReturn(new String[0]);
        when(constraintAnnotation.blockTemporaryEmails()).thenReturn(true);
        when(constraintAnnotation.corporateOnly()).thenReturn(false);
        validator.initialize(constraintAnnotation);
    }
}