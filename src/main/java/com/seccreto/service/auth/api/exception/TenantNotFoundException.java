package com.seccreto.service.auth.api.exception;

/**
 * Exceção para tenant não encontrado.
 * Usada quando um tenant específico não é encontrado no sistema.
 */
public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String message) {
        super(message);
    }
    
    public TenantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
