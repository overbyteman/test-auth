package com.seccreto.service.auth.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para controle de acesso baseado em tenant.
 * Verifica se o usuário tem acesso ao tenant do recurso.
 * 
 * Uso:
 * @RequireTenantAccess - Verifica se o usuário tem acesso ao tenant do recurso
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireTenantAccess {
    /**
     * Se true, permite apenas SUPER_ADMIN acessar qualquer tenant
     * Se false, permite ADMIN+ acessar apenas seu próprio tenant
     */
    boolean superAdminOnly() default false;
}
