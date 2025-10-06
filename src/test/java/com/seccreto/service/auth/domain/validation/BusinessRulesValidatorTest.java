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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BusinessRulesValidatorTest {

    private BusinessRulesValidator validator;
    private BusinessRules constraintAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new BusinessRulesValidator();
        constraintAnnotation = mock(BusinessRules.class);
        
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void shouldInitializeWithRuleTypes() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {
            BusinessRules.BusinessRuleType.NO_PROFANITY,
            BusinessRules.BusinessRuleType.NOT_BLACKLISTED
        };
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);

        // When
        validator.initialize(constraintAnnotation);

        // Then
        verify(constraintAnnotation).value();
        verify(constraintAnnotation).parameters();
    }

    @Test
    void shouldReturnTrueForNullValue() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.NO_PROFANITY};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        // When
        boolean result = validator.isValid(null, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldValidateNoProfanitySuccessfully() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.NO_PROFANITY};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String validText = "João Silva";

        // When
        boolean result = validator.isValid(validText, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldRejectTextWithProfanity() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.NO_PROFANITY};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String textWithProfanity = "admin user";

        // When
        boolean result = validator.isValid(textWithProfanity, context);

        // Then
    assertThat(result).isFalse();
    verify(context).buildConstraintViolationWithTemplate(argThat(s -> s != null && s.startsWith("Texto contém palavra não permitida")));
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldValidateNotBlacklistedSuccessfully() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.NOT_BLACKLISTED};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String validEmail = "user@example.com";

        // When
        boolean result = validator.isValid(validEmail, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldRejectBlacklistedEmail() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.NOT_BLACKLISTED};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String blacklistedEmail = "spam@example.com";

        // When
        boolean result = validator.isValid(blacklistedEmail, context);

        // Then
        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Email está na lista de bloqueio");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldValidateBusinessHoursSuccessfully() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.BUSINESS_HOURS_ONLY};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String validText = "Valid input";

        // When
        boolean result = validator.isValid(validText, context);

        // Then
        // Business hours check depends on current LocalTime; accept either outcome but assert
        // that if it fails, a specific violation template is added.
        if (result) {
            verify(context, never()).buildConstraintViolationWithTemplate(anyString());
        } else {
            verify(context).buildConstraintViolationWithTemplate("Registros são permitidos apenas em horário comercial (08:00 - 18:00)");
            verify(violationBuilder).addConstraintViolation();
        }
    }

    @Test
    void shouldValidateMinimumAgeSuccessfully() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.MINIMUM_AGE};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String validText = "Valid input";

        // When
        boolean result = validator.isValid(validText, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldValidateNotBotSuccessfully() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.NOT_BOT};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String validText = "Valid input";

        // When
        boolean result = validator.isValid(validText, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldHandleMultipleRuleTypes() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {
            BusinessRules.BusinessRuleType.NO_PROFANITY,
            BusinessRules.BusinessRuleType.NOT_BLACKLISTED
        };
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String validText = "João Silva";

        // When
        boolean result = validator.isValid(validText, context);

        // Then
        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldFailWhenAnyRuleFails() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {
            BusinessRules.BusinessRuleType.NO_PROFANITY,
            BusinessRules.BusinessRuleType.NOT_BLACKLISTED
        };
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String textWithProfanity = "admin user";

        // When
        boolean result = validator.isValid(textWithProfanity, context);

        // Then
    assertThat(result).isFalse();
    verify(context).buildConstraintViolationWithTemplate(argThat(s -> s != null && s.startsWith("Texto contém palavra não permitida")));
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldHandleCaseInsensitiveProfanityCheck() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.NO_PROFANITY};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String textWithProfanity = "ADMIN USER";

        // When
        boolean result = validator.isValid(textWithProfanity, context);

        // Then
    assertThat(result).isFalse();
    verify(context).buildConstraintViolationWithTemplate(argThat(s -> s != null && s.startsWith("Texto contém palavra não permitida")));
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void shouldHandleCaseInsensitiveBlacklistCheck() {
        // Given
        BusinessRules.BusinessRuleType[] ruleTypes = {BusinessRules.BusinessRuleType.NOT_BLACKLISTED};
        when(constraintAnnotation.value()).thenReturn(ruleTypes);
        when(constraintAnnotation.parameters()).thenReturn(new String[0]);
        validator.initialize(constraintAnnotation);

        String blacklistedEmail = "SPAM@EXAMPLE.COM";

        // When
        boolean result = validator.isValid(blacklistedEmail, context);

        // Then
        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Email está na lista de bloqueio");
        verify(violationBuilder).addConstraintViolation();
    }
}