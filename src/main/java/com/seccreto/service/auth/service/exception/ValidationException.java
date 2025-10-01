package com.seccreto.service.auth.service.exception;

/**
 * Exceção para erros de validação de dados de entrada.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}

