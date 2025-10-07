package com.seccreto.service.auth.config;

import com.seccreto.service.auth.service.authorization.AuthorizationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aspect para controle de acesso baseado em anotações customizadas
 */
@Aspect
@Component
public class AuthorizationAspect {

    @Autowired
    private AuthorizationService authorizationService;

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

        if (authorizationService.hasRole("SUPER_ADMIN")) {
            return joinPoint.proceed();
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

    @Around("@annotation(requireTenantAccess)")
    public Object checkTenantAccess(ProceedingJoinPoint joinPoint, RequireTenantAccess requireTenantAccess) throws Throwable {
        // Extrai o tenantId dos parâmetros do método
        Object[] args = joinPoint.getArgs();
        UUID tenantId = null;
        
        // Procura por tenantId nos parâmetros
        for (Object arg : args) {
            if (arg instanceof UUID) {
                // Assumindo que o primeiro UUID é o tenantId
                tenantId = (UUID) arg;
                break;
            }
        }

        if (tenantId == null) {
            throw new AccessDeniedException("Tenant ID não encontrado nos parâmetros");
        }

        if (!authorizationService.hasTenantAccess(tenantId)) {
            throw new AccessDeniedException("Acesso negado ao tenant: " + tenantId);
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(requireOwnershipOrRole)")
    public Object checkOwnershipOrRole(ProceedingJoinPoint joinPoint, RequireOwnershipOrRole requireOwnershipOrRole) throws Throwable {
        // Extrai o userId dos parâmetros do método
        Object[] args = joinPoint.getArgs();
        UUID userId = null;
        
        // Procura por userId nos parâmetros
        for (Object arg : args) {
            if (arg instanceof UUID) {
                // Assumindo que o primeiro UUID é o userId
                userId = (UUID) arg;
                break;
            }
        }

        if (userId == null) {
            throw new AccessDeniedException("User ID não encontrado nos parâmetros");
        }

        if (!authorizationService.hasOwnershipOrRole(userId, requireOwnershipOrRole.value())) {
            throw new AccessDeniedException("Acesso negado. Você só pode acessar seus próprios dados ou ter roles: " + Arrays.toString(requireOwnershipOrRole.value()));
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(requireSelfOnly)")
    public Object checkSelfOnly(ProceedingJoinPoint joinPoint, RequireSelfOnly requireSelfOnly) throws Throwable {
        // Extrai o userId dos parâmetros do método
        Object[] args = joinPoint.getArgs();
        UUID userId = null;
        
        // Procura por userId nos parâmetros
        for (Object arg : args) {
            if (arg instanceof UUID) {
                // Assumindo que o primeiro UUID é o userId
                userId = (UUID) arg;
                break;
            }
        }

        if (userId == null) {
            throw new AccessDeniedException("User ID não encontrado nos parâmetros");
        }

        if (!authorizationService.isOwner(userId)) {
            throw new AccessDeniedException("Acesso negado. Você só pode acessar seus próprios dados");
        }

        return joinPoint.proceed();
    }
}