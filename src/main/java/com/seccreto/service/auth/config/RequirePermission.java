package com.seccreto.service.auth.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controle de acesso baseado em permissions.
 * 
 * Uso:
 * @RequirePermission("create:users")
 * @RequirePermission({"read:users", "update:users"}) // Qualquer uma das permissions
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value();
    boolean requireAll() default false; // Se true, requer todas as permissions; se false, requer apenas uma
}
