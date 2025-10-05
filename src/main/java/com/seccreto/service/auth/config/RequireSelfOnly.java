package com.seccreto.service.auth.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controle de acesso que permite apenas o próprio usuário.
 * 
 * Uso:
 * @RequireSelfOnly - Apenas o próprio usuário pode acessar
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSelfOnly {
}
