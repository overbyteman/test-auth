package com.seccreto.service.auth.api.exception;

/**
 * Exceção para sessões expiradas.
 * Usada quando uma sessão é acessada após sua expiração.
 */
public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException(String message) {
        super(message);
    }
    
    public SessionExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
