package com.seccreto.service.auth.domain.security;

import com.seccreto.service.auth.domain.exceptions.DomainException;

import java.util.List;

/**
 * Exceção lançada quando a validação de entrada falha
 */
public class InputValidationException extends DomainException {

    private final String input;
    private final String validationType;

    public InputValidationException(
            final String input,
            final String validationType,
            final String message) {
        super(message, List.of());
        this.input = input;
        this.validationType = validationType;
    }

    public static InputValidationException sqlInjectionDetected(String input) {
        final String message = String.format(
                "SQL injection pattern detected in input: %s",
                input.length() > 100 ? input.substring(0, 100) + "..." : input
        );
        return new InputValidationException(input, "SQL_INJECTION", message);
    }

    public static InputValidationException xssDetected(String input) {
        final String message = String.format(
                "XSS pattern detected in input: %s",
                input.length() > 100 ? input.substring(0, 100) + "..." : input
        );
        return new InputValidationException(input, "XSS", message);
    }

    public static InputValidationException injectionDetected(String input) {
        final String message = String.format(
                "Injection pattern detected in input: %s",
                input.length() > 100 ? input.substring(0, 100) + "..." : input
        );
        return new InputValidationException(input, "INJECTION", message);
    }

    public static InputValidationException unsafeInput(String input) {
        final String message = String.format(
                "Unsafe input detected: %s",
                input.length() > 100 ? input.substring(0, 100) + "..." : input
        );
        return new InputValidationException(input, "UNSAFE", message);
    }

    public String getInput() {
        return input;
    }

    public String getValidationType() {
        return validationType;
    }
}
