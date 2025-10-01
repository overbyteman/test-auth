package com.seccreto.service.auth.api.exception;

/**
 * Exceção para permissões insuficientes.
 * Usada quando um usuário não tem permissões suficientes para realizar uma operação.
 */
public class InsufficientPermissionsException extends RuntimeException {
    public InsufficientPermissionsException(String message) {
        super(message);
    }
    
    public InsufficientPermissionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
