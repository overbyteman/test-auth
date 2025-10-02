package com.seccreto.service.auth.model.sessions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Classe que representa uma sessão de usuário no sistema (JPA Entity)
 *
 * Características de implementação sênior:
 * - JPA Entity com mapeamento automático
 * - Gerenciamento de refresh tokens
 * - Rastreamento de user agent e IP
 * - Controle de expiração de sessões
 * - Timestamps automáticos com Hibernate
 * - Validações de negócio
 * - Documentação completa com Swagger
 * - Lombok para redução de boilerplate
 */
@Entity
@Table(name = "sessions")
@Schema(description = "Entidade que representa uma sessão de usuário no sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"refreshTokenHash"})
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @Schema(description = "Identificador único da sessão (UUID)")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @Schema(description = "ID do usuário proprietário da sessão (UUID)")
    private UUID userId;

    @Column(name = "refresh_token_hash", nullable = false)
    @Schema(description = "Hash do refresh token")
    @JsonIgnore
    private String refreshTokenHash;

    @Column(name = "user_agent")
    @Schema(description = "User agent do cliente", example = "Mozilla/5.0...")
    private String userAgent;

    @Column(name = "ip_address")
    @Schema(description = "Endereço IP do cliente")
    private InetAddress ipAddress;

    @Column(name = "expires_at", nullable = false)
    @Schema(description = "Data e hora de expiração da sessão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Data e hora de criação da sessão")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Construtor para criação de novas sessões
     * CreatedAt é gerenciado automaticamente pelo Hibernate
     */
    public static Session createNew(UUID userId, String refreshTokenHash, String userAgent,
                                    InetAddress ipAddress, LocalDateTime expiresAt) {
        return Session.builder()
                .userId(userId)
                .refreshTokenHash(refreshTokenHash)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Construtor para criação de novas sessões com IP como String
     */
    public static Session createNew(UUID userId, String token, String ipAddress, String userAgent) {
        try {
            InetAddress ip = InetAddress.getByName(ipAddress);
            return createNew(userId, token, userAgent, ip, LocalDateTime.now().plusHours(24));
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