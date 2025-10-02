package com.seccreto.service.auth.model.audit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade para logs de auditoria do sistema.
 * 
 * Registra todas as ações sensíveis realizadas no sistema para compliance e segurança.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id")
})
@Schema(description = "Log de auditoria para ações do sistema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "ID único do log de auditoria")
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id")
    @Schema(description = "ID do usuário que realizou a ação")
    private UUID userId;

    @Column(name = "session_id")
    @Schema(description = "ID da sessão")
    private UUID sessionId;

    @Column(name = "action", nullable = false, length = 100)
    @Schema(description = "Ação realizada", example = "LOGIN")
    private String action;

    @Column(name = "resource_type", length = 50)
    @Schema(description = "Tipo do recurso afetado", example = "USER")
    private String resourceType;

    @Column(name = "resource_id")
    @Schema(description = "ID do recurso afetado")
    private UUID resourceId;

    @Column(name = "details", columnDefinition = "TEXT")
    @Schema(description = "Detalhes adicionais da ação")
    private String details;

    @Column(name = "ip_address", length = 45)
    @Schema(description = "Endereço IP de origem")
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    @Schema(description = "User Agent do cliente")
    private String userAgent;

    @Column(name = "success", nullable = false)
    @Schema(description = "Se a ação foi bem-sucedida")
    private Boolean success;

    @Column(name = "error_message", length = 1000)
    @Schema(description = "Mensagem de erro (se houver)")
    private String errorMessage;

    @Column(name = "timestamp", nullable = false)
    @Schema(description = "Timestamp da ação")
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (success == null) {
            success = true;
        }
    }

    /**
     * Factory method para criar log de sucesso
     */
    public static AuditLog success(UUID userId, UUID sessionId, String action, 
                                  String resourceType, UUID resourceId, 
                                  String details, String ipAddress, String userAgent) {
        return AuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para criar log de erro
     */
    public static AuditLog failure(UUID userId, UUID sessionId, String action,
                                  String resourceType, UUID resourceId,
                                  String details, String errorMessage,
                                  String ipAddress, String userAgent) {
        return AuditLog.builder()
                .userId(userId)
                .sessionId(sessionId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .errorMessage(errorMessage)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para ações de sistema (sem usuário)
     */
    public static AuditLog system(String action, String details) {
        return AuditLog.builder()
                .action(action)
                .details(details)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
