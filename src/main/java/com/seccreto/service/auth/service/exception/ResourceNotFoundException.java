package com.seccreto.service.auth.service.exception;

/**
 * Exceção para indicar que um recurso não foi encontrado.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

