package com.seccreto.service.auth.service.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Implementação real do serviço JWT usando JJWT.
 */
@Service
@Profile({"postgres", "test", "dev", "stage", "prod"})
public class JwtServiceImpl implements JwtService {
    
    private final SecretKey secretKey;
    private final int accessTokenValidityHours;
    private final int refreshTokenValidityDays;
    
    public JwtServiceImpl(
            @Value("${jwt.secret:default-secret-key-change-in-production-environment}") String secret,
            @Value("${jwt.expiration:3600000}") int accessTokenExpiration,
            @Value("${jwt.refresh-expiration:604800000}") int refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityHours = accessTokenExpiration / (1000 * 60 * 60); // Convert ms to hours
        this.refreshTokenValidityDays = refreshTokenExpiration / (1000 * 60 * 60 * 24); // Convert ms to days
    }
    
    @Override
    public String generateAccessToken(UUID userId, UUID sessionId, UUID tenantId, List<String> roles, List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (accessTokenValidityHours * 60 * 60 * 1000L));
        
        JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("sessionId", sessionId.toString())
                .claim("type", "access")
                .claim("roles", roles)
                .claim("permissions", permissions)
                .signWith(secretKey);
        
        if (tenantId != null) {
            builder.claim("tenantId", tenantId.toString());
        }
        
        return builder.compact();
    }
    
    @Override
    public String generateRefreshToken(UUID userId, UUID sessionId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (refreshTokenValidityDays * 24 * 60 * 60 * 1000L));
        
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("sessionId", sessionId.toString())
                .claim("type", "refresh")
                .signWith(secretKey)
                .compact();
    }
    
    @Override
    public JwtValidationResult validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return new JwtValidationResult(
                false, null, null, null, List.of(), List.of(), null, "Token não fornecido"
            );
        }
        
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            UUID userId = UUID.fromString(claims.getSubject());
            UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));
            
            String tenantIdStr = claims.get("tenantId", String.class);
            UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
            
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            if (roles == null) roles = List.of();
            
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);
            if (permissions == null) permissions = List.of();
            
            LocalDateTime expiresAt = LocalDateTime.ofInstant(
                claims.getExpiration().toInstant(), java.time.ZoneId.systemDefault()
            );
            
            return new JwtValidationResult(
                true, userId, sessionId, tenantId, roles, permissions, expiresAt, null
            );
            
        } catch (ExpiredJwtException e) {
            UUID userId = null;
            UUID sessionId = null;
            LocalDateTime expiresAt = null;
            
            try {
                userId = UUID.fromString(e.getClaims().getSubject());
                sessionId = UUID.fromString(e.getClaims().get("sessionId", String.class));
                expiresAt = LocalDateTime.ofInstant(
                    e.getClaims().getExpiration().toInstant(), java.time.ZoneId.systemDefault()
                );
            } catch (Exception ignored) {
                // Ignore parsing errors for expired token
            }
            
            return new JwtValidationResult(
                false, userId, sessionId, null, List.of(), List.of(), expiresAt, "Token expirado"
            );
            
        } catch (UnsupportedJwtException e) {
            return new JwtValidationResult(
                false, null, null, null, List.of(), List.of(), null, "Token não suportado"
            );
            
        } catch (MalformedJwtException e) {
            return new JwtValidationResult(
                false, null, null, null, List.of(), List.of(), null, "Token malformado"
            );
            
        } catch (SecurityException | IllegalArgumentException | io.jsonwebtoken.security.SignatureException e) {
            return new JwtValidationResult(
                false, null, null, null, List.of(), List.of(), null, "Token inválido"
            );
        }
    }
    
    @Override
    public JwtTokenInfo extractTokenInfo(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Parse without validation (for expired tokens)
            String[] chunks = token.split("\\.");
            if (chunks.length != 3) {
                return null;
            }
            
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            UUID userId = UUID.fromString(claims.getSubject());
            UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));
            
            String tenantIdStr = claims.get("tenantId", String.class);
            UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
            
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            if (roles == null) roles = List.of();
            
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);
            if (permissions == null) permissions = List.of();
            
            LocalDateTime issuedAt = LocalDateTime.ofInstant(
                claims.getIssuedAt().toInstant(), java.time.ZoneId.systemDefault()
            );
            LocalDateTime expiresAt = LocalDateTime.ofInstant(
                claims.getExpiration().toInstant(), java.time.ZoneId.systemDefault()
            );
            
            return new JwtTokenInfo(
                userId, sessionId, tenantId, roles, permissions, issuedAt, expiresAt
            );
            
        } catch (Exception e) {
            // Try to extract from expired token
            try {
                int i = token.lastIndexOf('.');
                String withoutSignature = token.substring(0, i + 1);
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseUnsecuredClaims(withoutSignature)
                        .getPayload();
                
                UUID userId = UUID.fromString(claims.getSubject());
                UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));
                
                String tenantIdStr = claims.get("tenantId", String.class);
                UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
                
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                if (roles == null) roles = List.of();
                
                @SuppressWarnings("unchecked")
                List<String> permissions = claims.get("permissions", List.class);
                if (permissions == null) permissions = List.of();
                
                LocalDateTime issuedAt = LocalDateTime.ofInstant(
                    claims.getIssuedAt().toInstant(), java.time.ZoneId.systemDefault()
                );
                LocalDateTime expiresAt = LocalDateTime.ofInstant(
                    claims.getExpiration().toInstant(), java.time.ZoneId.systemDefault()
                );
                
                return new JwtTokenInfo(
                    userId, sessionId, tenantId, roles, permissions, issuedAt, expiresAt
                );
                
            } catch (Exception ignored) {
                return null;
            }
        }
    }
    
    @Override
    public boolean isTokenNearExpiry(String token, int minutesThreshold) {
        JwtTokenInfo tokenInfo = extractTokenInfo(token);
        if (tokenInfo == null) {
            return true; // Token inválido, considerar como próximo do vencimento
        }
        
        LocalDateTime thresholdTime = LocalDateTime.now().plusMinutes(minutesThreshold);
        return tokenInfo.expiresAt().isBefore(thresholdTime);
    }
}
