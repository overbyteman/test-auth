package com.seccreto.service.auth.api.exception;

/**
 * Exceção para tokens inválidos.
 * Usada quando um token de autenticação é inválido, expirado ou malformado.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
