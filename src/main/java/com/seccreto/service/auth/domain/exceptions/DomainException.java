package com.seccreto.service.auth.domain.exceptions;

import java.util.List;

/**
 * Exceção base para erros de domínio
 */
public abstract class DomainException extends RuntimeException {
    
    private final List<String> errors;
    
    protected DomainException(String message, List<String> errors) {
        super(message);
        this.errors = errors != null ? errors : List.of();
    }
    
    protected DomainException(String message, Throwable cause, List<String> errors) {
        super(message, cause);
        this.errors = errors != null ? errors : List.of();
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
