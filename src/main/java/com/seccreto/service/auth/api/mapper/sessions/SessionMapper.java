package com.seccreto.service.auth.api.mapper.sessions;

import com.seccreto.service.auth.api.dto.sessions.SessionRequest;
import com.seccreto.service.auth.api.dto.sessions.SessionResponse;
import com.seccreto.service.auth.model.sessions.Session;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper para conversão entre DTOs e entidades de Session.
 * Implementa o padrão de mapeamento estático para performance.
 */
public final class SessionMapper {
    private SessionMapper() {}

    /**
     * Converte SessionRequest para entidade Session.
     */
    public static Session toEntity(SessionRequest request) {
        if (request == null) {
            return null;
        }
        
        InetAddress ipAddress = null;
        if (request.getIpAddress() != null && !request.getIpAddress().trim().isEmpty()) {
            try {
                ipAddress = InetAddress.getByName(request.getIpAddress());
            } catch (Exception e) {
                // Log warning but don't fail
                System.err.println("Erro ao converter IP address: " + request.getIpAddress());
            }
        }
        
        return Session.builder()
                .userId(request.getUserId())
                .refreshTokenHash(request.getRefreshTokenHash())
                .userAgent(request.getUserAgent())
                .ipAddress(ipAddress)
                .expiresAt(request.getExpiresAt())
                .build();
    }

    /**
     * Converte entidade Session para SessionResponse.
     */
    public static SessionResponse toResponse(Session session) {
        if (session == null) {
            return null;
        }
        
        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .userAgent(session.getUserAgent())
                .ipAddress(session.getIpAddress() != null ? session.getIpAddress().getHostAddress() : null)
                .expiresAt(session.getExpiresAt())
                .createdAt(session.getCreatedAt())
                .isValid(session.isValid())
                .build();
    }

    /**
     * Converte lista de entidades Session para lista de SessionResponse.
     */
    public static List<SessionResponse> toResponseList(List<Session> sessions) {
        if (sessions == null) {
            return null;
        }
        
        return sessions.stream()
                .map(SessionMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atualiza entidade Session com dados do SessionRequest.
     */
    public static void updateEntity(Session session, SessionRequest request) {
        if (session == null || request == null) {
            return;
        }
        
        session.setUserId(request.getUserId());
        session.setRefreshTokenHash(request.getRefreshTokenHash());
        session.setUserAgent(request.getUserAgent());
        
        if (request.getIpAddress() != null && !request.getIpAddress().trim().isEmpty()) {
            try {
                session.setIpAddress(InetAddress.getByName(request.getIpAddress()));
            } catch (Exception e) {
                // Log warning but don't fail
                System.err.println("Erro ao converter IP address: " + request.getIpAddress());
            }
        }
        
        session.setExpiresAt(request.getExpiresAt());
    }
}
