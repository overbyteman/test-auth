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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordStrengthValidatorTest {

    private PasswordStrengthValidator validator;
    private PasswordStrength annotation;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new PasswordStrengthValidator();
        annotation = mock(PasswordStrength.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void initializeShouldReadConfiguration() {
        stubAnnotation(true);

        validator.initialize(annotation);

        verify(annotation).minLength();
        verify(annotation).maxLength();
        verify(annotation).requireUppercase();
        verify(annotation).requireLowercase();
        verify(annotation).requireDigit();
        verify(annotation).requireSpecialChar();
        verify(annotation).disallowCommon();
        verify(annotation).disallowSequences();
        verify(annotation).disallowRepeats();
    }

    @Test
    void isValidShouldReturnTrueWhenPasswordIsNull() {
        initializeDefaults();

        assertThat(validator.isValid(null, context)).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void isValidShouldRejectShortPassword() {
        initializeDefaults();

        boolean result = validator.isValid("Ab1!", context);

        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Senha deve ter pelo menos 8 caracteres");
    }

    @Test
    void isValidShouldRejectMissingUppercase() {
        initializeDefaults();

        boolean result = validator.isValid("mystr0ng!pass", context);

        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Senha deve conter pelo menos uma letra maiúscula");
    }

    @Test
    void isValidShouldRejectMissingLowercase() {
        initializeDefaults();

        boolean result = validator.isValid("MYSTR0NG!PASS", context);

        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Senha deve conter pelo menos uma letra minúscula");
    }

    @Test
    void isValidShouldRejectMissingDigit() {
        initializeDefaults();

        boolean result = validator.isValid("MyStrong!Pass", context);

        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Senha deve conter pelo menos um número");
    }

    @Test
    void isValidShouldRejectMissingSpecialCharacter() {
        initializeDefaults();

        boolean result = validator.isValid("MyStr0ngPass", context);

        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate(
                "Senha deve conter pelo menos um caractere especial (!@#$%^&*()_+-=[]{}|;':\"\\,.<>?/)");
    }

    @Test
    void isValidShouldRejectCommonPasswords() {
        stubAnnotation(true);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecialChar()).thenReturn(false);
        validator.initialize(annotation);

        boolean result = validator.isValid("password123", context);

        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Senha muito comum. Escolha uma senha mais segura");
    }

    @Test
    void isValidShouldRejectSequentialPasswords() {
        initializeDefaults();

        boolean result = validator.isValid("Abcdef12!", context);

        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Senha não pode conter sequências (123, abc, etc.)");
    }

    @Test
    void isValidShouldRejectRepeatingCharacters() {
        initializeDefaults();

        boolean result = validator.isValid("AAAbbb1!", context);

        assertThat(result).isFalse();
        verify(context).buildConstraintViolationWithTemplate("Senha não pode conter muitos caracteres repetidos consecutivos");
    }

    @Test
    void isValidShouldAcceptStrongPassword() {
        initializeDefaults();

        boolean result = validator.isValid("MyStr0ng!Pass", context);

        assertThat(result).isTrue();
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    private void initializeDefaults() {
        stubAnnotation(true);
        validator.initialize(annotation);
    }

    private void stubAnnotation(boolean strict) {
        when(annotation.minLength()).thenReturn(8);
        when(annotation.maxLength()).thenReturn(128);
        when(annotation.requireUppercase()).thenReturn(true);
        when(annotation.requireLowercase()).thenReturn(true);
        when(annotation.requireDigit()).thenReturn(true);
        when(annotation.requireSpecialChar()).thenReturn(true);
        when(annotation.disallowCommon()).thenReturn(strict);
        when(annotation.disallowSequences()).thenReturn(strict);
        when(annotation.disallowRepeats()).thenReturn(strict);
    }
}