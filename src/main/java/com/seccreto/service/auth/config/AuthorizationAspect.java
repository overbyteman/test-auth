package com.seccreto.service.auth.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aspect para processar anotações de autorização customizadas.
 * 
 * Processa @RequireRole e @RequirePermission para controle de acesso
 * baseado em roles e permissions do usuário autenticado.
 */
@Aspect
@Component
public class AuthorizationAspect {

    @Around("@annotation(requireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        Set<String> userRoles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5)) // Remove "ROLE_" prefix
                .collect(Collectors.toSet());

        String[] requiredRoles = requireRole.value();
        
        boolean hasAccess;
        if (requireRole.requireAll()) {
            // Requer todos os roles
            hasAccess = userRoles.containsAll(Arrays.asList(requiredRoles));
        } else {
            // Requer pelo menos um role
            hasAccess = Arrays.stream(requiredRoles)
                    .anyMatch(userRoles::contains);
        }

        if (!hasAccess) {
            throw new AccessDeniedException("Acesso negado. Roles necessários: " + Arrays.toString(requiredRoles));
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        Set<String> userPermissions = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith("ROLE_"))
                .collect(Collectors.toSet());

        String[] requiredPermissions = requirePermission.value();
        
        boolean hasAccess;
        if (requirePermission.requireAll()) {
            // Requer todas as permissions
            hasAccess = userPermissions.containsAll(Arrays.asList(requiredPermissions));
        } else {
            // Requer pelo menos uma permission
            hasAccess = Arrays.stream(requiredPermissions)
                    .anyMatch(userPermissions::contains);
        }

        if (!hasAccess) {
            throw new AccessDeniedException("Acesso negado. Permissões necessárias: " + Arrays.toString(requiredPermissions));
        }

        return joinPoint.proceed();
    }
}
