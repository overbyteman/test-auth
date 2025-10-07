package com.seccreto.service.auth.api.dto.permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Schema(description = "Representação de uma política de segurança pré-definida para utilização na criação de permissões")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionPolicyPresetResponse {
    @Schema(description = "Código único da política", example = "admin-full-access")
    private String code;

    @Schema(description = "Nome descritivo da política", example = "Admin Full Access")
    private String name;

    @Schema(description = "Descrição resumida da política", example = "Controle total sobre operações críticas")
    private String description;

    @Schema(description = "Efeito padrão da política", example = "ALLOW")
    private String effect;

    @Schema(description = "Lista de ações recomendadas associadas à política")
    private List<String> recommendedActions;

    @Schema(description = "Lista de recursos recomendados associadas à política")
    private List<String> recommendedResources;

    @Schema(description = "Faixas de IP recomendadas no formato CIDR", example = "[\"10.0.0.0/16\", \"192.168.0.0/24\"]")
    private List<String> recommendedIpRanges;

    @Schema(description = "Janelas de horário recomendadas para aplicação da policy")
    private List<ScheduleWindow> recommendedSchedules;

    @Schema(description = "Outras condições recomendadas em formato chave/valor")
    private Map<String, Object> additionalContext;

    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleWindow {
        @Schema(description = "Timezone IANA", example = "America/Sao_Paulo")
        private String timezone;

        @Schema(description = "Dias da semana contemplados", example = "[\"MONDAY\", \"TUESDAY\", \"WEDNESDAY\", \"THURSDAY\", \"FRIDAY\"]")
        private List<String> days;

        @Schema(description = "Horário inicial no formato HH:mm", example = "08:00")
        private String start;

        @Schema(description = "Horário final no formato HH:mm", example = "20:00")
        private String end;
    }
}
