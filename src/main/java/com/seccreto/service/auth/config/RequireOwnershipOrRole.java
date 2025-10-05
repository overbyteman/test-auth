package com.seccreto.service.auth.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controle de acesso que permite:
 * 1. O próprio usuário acessar seus dados
 * 2. Usuários com roles específicos acessarem qualquer usuário
 * 
 * Uso:
 * @RequireOwnershipOrRole({"ADMIN", "MANAGER"}) - Permite o próprio usuário ou ADMIN/MANAGER
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireOwnershipOrRole {
    String[] value();
    boolean requireAll() default false;
}
