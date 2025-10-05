package com.seccreto.service.auth.service.authorization;

import com.seccreto.service.auth.service.users_tenants_roles.UsersTenantsRolesService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço para verificação de autorização baseada em tenant e ownership
 */
@Service
public class AuthorizationService {

    private final UsersTenantsRolesService usersTenantsRolesService;

    public AuthorizationService(UsersTenantsRolesService usersTenantsRolesService) {
        this.usersTenantsRolesService = usersTenantsRolesService;
    }

    /**
     * Verifica se o usuário atual tem acesso ao tenant do recurso
     */
    public boolean hasTenantAccess(UUID resourceTenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        // SUPER_ADMIN pode acessar qualquer tenant
        if (hasRole("SUPER_ADMIN")) {
            return true;
        }

        // Outros usuários só podem acessar seus próprios tenants
        UUID currentUserId = getCurrentUserId(auth);
        if (currentUserId == null) {
            return false;
        }

        return usersTenantsRolesService.findRolesByUserAndTenant(currentUserId, resourceTenantId)
                .stream()
                .anyMatch(utr -> utr.getTenantId().equals(resourceTenantId));
    }

    /**
     * Verifica se o usuário atual é o dono do recurso ou tem roles específicos
     */
    public boolean hasOwnershipOrRole(UUID resourceUserId, String[] requiredRoles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        UUID currentUserId = getCurrentUserId(auth);
        if (currentUserId == null) {
            return false;
        }

        // Verifica se é o próprio usuário
        if (currentUserId.equals(resourceUserId)) {
            return true;
        }

        // Verifica se tem algum dos roles necessários
        return hasAnyRole(requiredRoles);
    }

    /**
     * Verifica se o usuário atual é o dono do recurso
     */
    public boolean isOwner(UUID resourceUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID currentUserId = getCurrentUserId(auth);
        if (currentUserId == null) {
            return false;
        }

        return currentUserId.equals(resourceUserId);
    }

    /**
     * Verifica se o usuário tem um role específico
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Verifica se o usuário tem algum dos roles especificados
     */
    public boolean hasAnyRole(String[] roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Set<String> userRoles = auth.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5)) // Remove "ROLE_" prefix
                .collect(Collectors.toSet());

        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Obtém o usuário atual do contexto de segurança
     */
    private UUID getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof UUID uuid) {
            return uuid;
        }

        if (principal instanceof String principalString) {
            try {
                return UUID.fromString(principalString);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        // Assumindo que o principal pode conter o User
        if (principal instanceof com.seccreto.service.auth.model.users.User user) {
            return user.getId();
        }

        return null;
    }
}
