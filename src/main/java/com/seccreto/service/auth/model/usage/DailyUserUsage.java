package com.seccreto.service.auth.model.usage;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_user_usage")
@IdClass(DailyUserUsageId.class)
@Schema(description = "Agregação diária de uso por usuário e tenant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DailyUserUsage {
    @Id
    @Column(name = "usage_date")
    @Schema(description = "Data de uso (usage_date)")
    @EqualsAndHashCode.Include
    private LocalDate usageDate;

    @Id
    @Column(name = "user_id")
    @Schema(description = "ID do usuário (UUID)")
    @EqualsAndHashCode.Include
    private UUID userId;

    @Id
    @Column(name = "tenant_id")
    @Schema(description = "ID do tenant (UUID)")
    @EqualsAndHashCode.Include
    private UUID tenantId;

    @Column(name = "logins")
    @Schema(description = "Quantidade de logins no dia")
    private Long logins;

    @Column(name = "actions")
    @Schema(description = "Quantidade de ações no dia")
    private Long actions;

    @Column(name = "last_action_at")
    @Schema(description = "Última ação registrada")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActionAt;

    @Column(name = "created_at")
    @Schema(description = "Criado em")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Schema(description = "Atualizado em")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static DailyUserUsage createNew(UUID userId, UUID tenantId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        return DailyUserUsage.builder()
                .usageDate(date)
                .userId(userId)
                .tenantId(tenantId)
                .logins(0L)
                .actions(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void incrementLogin() { this.logins++; touch(); }
    public void incrementAction(LocalDateTime actionAt) {
        this.actions++;
        if (actionAt != null) {
            if (this.lastActionAt == null || actionAt.isAfter(this.lastActionAt)) {
                this.lastActionAt = actionAt;
            }
        }
        touch();
    }
    public void touch() { 
        this.updatedAt = LocalDateTime.now();
    }
}

