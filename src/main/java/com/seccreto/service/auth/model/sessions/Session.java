package com.seccreto.service.auth.model.sessions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.net.InetAddress;
import java.time.LocalDateTime;

/**
 * Classe que representa uma sessão de usuário no sistema (Model)
 * 
 * Características de implementação sênior:
 * - Gerenciamento de refresh tokens
 * - Rastreamento de user agent e IP
 * - Controle de expiração de sessões
 * - Timestamps com timezone
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Schema(description = "Entidade que representa uma sessão de usuário no sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"refreshTokenHash"})
public class Session {
    @Schema(description = "Identificador único da sessão", example = "1")
    @EqualsAndHashCode.Include
    private Long id;
    
    @Schema(description = "ID do usuário proprietário da sessão", example = "1")
    private Long userId;
    
    @Schema(description = "Hash do refresh token")
    @JsonIgnore
    private String refreshTokenHash;
    
    @Schema(description = "User agent do cliente", example = "Mozilla/5.0...")
    private String userAgent;
    
    @Schema(description = "Endereço IP do cliente")
    private InetAddress ipAddress;
    
    @Schema(description = "Data e hora de expiração da sessão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Data e hora de criação da sessão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Construtor para criação de novas sessões
     */
    public static Session createNew(Long userId, String refreshTokenHash, String userAgent, 
                                   InetAddress ipAddress, LocalDateTime expiresAt) {
        return Session.builder()
                .userId(userId)
                .refreshTokenHash(refreshTokenHash)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Construtor para criação de novas sessões com IP como String
     */
    public static Session createNew(Long userId, String token, String ipAddress, String userAgent) {
        try {
            InetAddress ip = InetAddress.getByName(ipAddress);
            return Session.builder()
                    .userId(userId)
                    .refreshTokenHash(token)
                    .userAgent(userAgent)
                    .ipAddress(ip)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("IP address inválido: " + ipAddress, e);
        }
    }
    
    /**
     * Verifica se a sessão está expirada
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Verifica se a sessão é válida (não expirada)
     */
    public boolean isValid() {
        return !isExpired();
    }
}
