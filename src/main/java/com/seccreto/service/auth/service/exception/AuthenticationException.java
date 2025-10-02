package com.seccreto.service.auth.service.exception;

/**
 * Exceção lançada quando há falha na autenticação.
 * Deve resultar em HTTP 401 Unauthorized.
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
