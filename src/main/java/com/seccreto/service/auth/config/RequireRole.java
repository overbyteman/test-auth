package com.seccreto.service.auth.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controle de acesso baseado em roles.
 * 
 * Uso:
 * @RequireRole("ADMIN")
 * @RequireRole({"ADMIN", "MANAGER"}) // Qualquer um dos roles
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
    boolean requireAll() default false; // Se true, requer todos os roles; se false, requer apenas um
}
