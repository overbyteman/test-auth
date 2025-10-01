package com.seccreto.service.auth.api.exception;

/**
 * Exceção base para erros de regras de negócio.
 * Usada para casos onde a operação não pode ser realizada devido a regras de negócio.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
