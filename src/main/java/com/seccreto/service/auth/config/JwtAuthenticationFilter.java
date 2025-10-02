package com.seccreto.service.auth.config;

import com.seccreto.service.auth.service.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filtro JWT para autenticação automática baseada em token.
 * 
 * Características:
 * - Extrai token do header Authorization
 * - Valida token usando JwtService
 * - Configura SecurityContext com usuário autenticado
 * - Suporte a roles e permissions
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            JwtService.JwtValidationResult validationResult = jwtService.validateToken(token);
            
            if (validationResult.valid() && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Criar authorities baseadas em roles e permissions
                List<SimpleGrantedAuthority> authorities = validationResult.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
                
                // Adicionar permissions como authorities
                validationResult.permissions().stream()
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);

                // Criar authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        validationResult.userId().toString(),
                        null,
                        authorities
                );
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Log error but don't block the request
            logger.debug("JWT validation failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Pular filtro para endpoints públicos
        return path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/api/auth/login") ||
               path.equals("/api/auth/register") ||
               path.equals("/api/auth/forgot-password") ||
               path.equals("/api/auth/reset-password") ||
               path.equals("/api/auth/refresh-token") ||
               path.endsWith("/health");
    }
}
