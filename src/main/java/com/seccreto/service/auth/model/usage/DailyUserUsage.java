package com.seccreto.service.auth.model.usage;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Agregação diária de uso por usuário e tenant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DailyUserUsage {
    @Schema(description = "Data de uso (usage_date)")
    @EqualsAndHashCode.Include
    private LocalDate usageDate;

    @Schema(description = "ID do usuário (UUID)")
    @EqualsAndHashCode.Include
    private UUID userId;

    @Schema(description = "ID do tenant (UUID)")
    @EqualsAndHashCode.Include
    private UUID tenantId;

    @Schema(description = "Quantidade de logins no dia")
    private int logins;

    @Schema(description = "Quantidade de ações no dia")
    private int actions;

    @Schema(description = "Timestamp da última ação registrada no dia")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActionAt;

    @Schema(description = "Criado em")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Atualizado em")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static DailyUserUsage createNew(UUID userId, UUID tenantId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        return DailyUserUsage.builder()
                .usageDate(date)
                .userId(userId)
                .tenantId(tenantId)
                .logins(0)
                .actions(0)
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
    public void touch() { this.updatedAt = LocalDateTime.now(); }
}

