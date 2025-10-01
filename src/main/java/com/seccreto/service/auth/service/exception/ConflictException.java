package com.seccreto.service.auth.service.exception;

/**
 * Exceção para conflitos de negócio (ex: duplicidade de dados).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}

