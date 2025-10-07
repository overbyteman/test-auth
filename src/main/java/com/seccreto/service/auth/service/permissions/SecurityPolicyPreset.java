package com.seccreto.service.auth.service.permissions;

import com.seccreto.service.auth.api.dto.permissions.PermissionPolicyPresetResponse;
import com.seccreto.service.auth.model.policies.PolicyEffect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catálogo fixo de policies de segurança aplicáveis a permissões de usuários.
 * Fonte: políticas semeadas em dummy.sql para os contextos principais da plataforma.
 */
public enum SecurityPolicyPreset {
    ADMIN_FULL_ACCESS(
            "admin-full-access",
            "Admin Full Access",
            "Controle total sobre operações críticas, configurações e gestão de usuários.",
            PolicyEffect.ALLOW,
            List.of("create", "read", "update", "delete", "manage"),
        List.of("users", "members", "classes", "payments", "equipment", "competitions", "reports", "settings", "permissions"),
        List.of("10.0.0.0/16", "192.168.0.0/24"),
        List.of(ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.ALL_DAYS, "00:00", "23:59")),
        Map.of(
            "mfa_required", true,
            "device_posture", "managed",
            "notes", "Uso exclusivo de redes internas ou VPN corporativa"
        )
    ),
    MANAGEMENT_ACCESS(
            "management-access",
            "Management Access",
            "Gestão executiva e supervisão operacional sem acesso a configurações críticas.",
            PolicyEffect.ALLOW,
            List.of("create", "read", "update", "delete"),
        List.of("members", "classes", "payments", "competitions", "reports", "equipment", "settings", "users", "permissions"),
        List.of("10.0.10.0/24", "10.0.20.0/24"),
        List.of(ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.BUSINESS_DAYS, "07:00", "22:00")),
        Map.of(
            "mfa_required", true,
            "risk_level", "medium"
        )
    ),
    FINANCIAL_ACCESS(
            "financial-access",
            "Financial Access",
            "Processamento financeiro, planos e relatórios sensíveis.",
            PolicyEffect.ALLOW,
            List.of("read", "create", "update"),
        List.of("payments", "invoices", "financial_reports", "members", "classes", "equipment", "competitions"),
        List.of("10.0.30.0/24", "172.16.30.0/24"),
        List.of(ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.BUSINESS_DAYS, "08:00", "18:30")),
        Map.of(
            "requires_dual_approval", true,
            "notes", "Aplicar segregação de funções para grandes valores"
        )
    ),
    OPERATIONS_ACCESS(
            "operations-access",
            "Operations Access",
            "Backoffice operacional para cadastros, aulas e suporte administrativo.",
            PolicyEffect.ALLOW,
            List.of("read", "update", "create"),
        List.of("members", "classes", "equipment", "payments", "competitions"),
        List.of("10.0.40.0/24", "172.16.20.0/24"),
        List.of(ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.EXTENDED_DAYS, "06:00", "22:00")),
        Map.of("department", "operations")
    ),
    RECEPTION_ACCESS(
            "reception-access",
            "Reception Access",
            "Atendimento ao público com foco em cadastros, agendas e relatórios básicos.",
            PolicyEffect.ALLOW,
            List.of("read", "create"),
        List.of("members", "classes", "payments", "schedules", "basic_reports", "competitions"),
        List.of("10.0.50.0/24"),
        List.of(ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.SEVEN_DAYS, "07:00", "21:30")),
        Map.of("department", "frontdesk")
    ),
    SECURITY_ACCESS(
            "security-access",
            "Security Access",
            "Controle de acesso físico, vigilância e logs.",
            PolicyEffect.ALLOW,
            List.of("read"),
        List.of("members", "classes", "equipment", "competitions", "access_logs", "facilities"),
        List.of("10.0.60.0/24", "172.16.40.0/24"),
        List.of(
            ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.SEVEN_DAYS, "00:00", "23:59"),
            ScheduleWindow.of("UTC", WorkweekDays.SEVEN_DAYS, "00:00", "23:59")
        ),
        Map.of(
            "mfa_required", true,
            "monitoring_level", "critical"
        )
    ),
    EQUIPMENT_MAINTENANCE(
            "equipment-maintenance",
            "Equipment Maintenance",
            "Gestão de inventário, manutenção e ciclo de vida dos equipamentos.",
            PolicyEffect.ALLOW,
            List.of("create", "read", "update", "delete"),
        List.of("equipment", "inventory"),
        List.of("10.0.70.0/24"),
        List.of(ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.BUSINESS_DAYS, "08:00", "18:00")),
        Map.of("safety_training_required", true)
    ),
    MEMBER_VIP_ACCESS(
            "member-vip-access",
            "Member VIP Access",
            "Benefícios premium, agendas exclusivas e acompanhamento personalizado.",
            PolicyEffect.ALLOW,
            List.of("read"),
        List.of("members", "classes", "competitions", "perks"),
        List.of("0.0.0.0/0"),
        List.of(ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.SEVEN_DAYS, "06:00", "23:00")),
        Map.of("tier", "vip")
    ),
    MEMBER_BASIC_ACCESS(
            "member-basic-access",
            "Member Basic Access",
            "Acesso padrão a aulas, competições e histórico básico.",
            PolicyEffect.ALLOW,
            List.of("read"),
        List.of("members", "classes", "competitions"),
        List.of("0.0.0.0/0"),
        List.of(ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.SEVEN_DAYS, "06:00", "22:00")),
        Map.of("tier", "regular")
    ),
    AFTER_HOURS_SUPPORT(
        "after-hours-support",
        "After Hours Support",
        "Suporte emergencial fora do expediente com escopo limitado.",
        PolicyEffect.ALLOW,
        List.of("read", "update"),
        List.of("members", "classes", "competitions", "incidents"),
        List.of("10.0.80.0/24"),
        List.of(
            ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.BUSINESS_DAYS, "18:00", "23:59"),
            ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.WEEKENDS, "08:00", "22:00")
        ),
        Map.of(
            "incident_priority", List.of("P0", "P1"),
            "notes", "Acesso restrito a situações de alta criticidade"
        )
    ),
    TRUSTED_NETWORK_ACCESS(
        "trusted-network-access",
        "Trusted Network Access",
        "Acesso somente a partir de redes corporativas confiáveis.",
        PolicyEffect.ALLOW,
        List.of("read"),
        List.of("reports", "payments", "members"),
        List.of("203.0.113.0/24", "198.51.100.0/24"),
        List.of(ScheduleWindow.of("America/Sao_Paulo", WorkweekDays.BUSINESS_DAYS, "07:00", "20:00")),
        Map.of(
            "mfa_required", true,
            "device_posture", "corporate-managed",
            "geo_restrictions", List.of("BR", "PT")
        )
    );

    private final String code;
    private final String name;
    private final String description;
    private final PolicyEffect effect;
    private final List<String> actions;
    private final List<String> resources;
    private final List<String> ipRanges;
    private final List<ScheduleWindow> scheduleWindows;
    private final Map<String, Object> additionalContext;

    SecurityPolicyPreset(String code,
             String name,
             String description,
             PolicyEffect effect,
             List<String> actions,
             List<String> resources,
             List<String> ipRanges,
             List<ScheduleWindow> scheduleWindows,
             Map<String, Object> additionalContext) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.effect = effect;
    this.actions = List.copyOf(actions);
    this.resources = List.copyOf(resources);
    this.ipRanges = ipRanges != null ? List.copyOf(ipRanges) : List.of();
    this.scheduleWindows = scheduleWindows != null ? List.copyOf(scheduleWindows) : List.of();
    this.additionalContext = additionalContext != null ? Map.copyOf(additionalContext) : Map.of();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PolicyEffect getEffect() {
        return effect;
    }

    public List<String> getActions() {
        return actions;
    }

    public List<String> getResources() {
        return resources;
    }

    public List<String> getIpRanges() {
        return ipRanges;
    }

    public List<ScheduleWindow> getScheduleWindows() {
        return scheduleWindows;
    }

    public Map<String, Object> getAdditionalContext() {
        return additionalContext;
    }

    public PermissionPolicyPresetResponse toResponse() {
        return PermissionPolicyPresetResponse.builder()
                .code(code)
                .name(name)
                .description(description)
                .effect(effect.name())
                .recommendedActions(actions)
                .recommendedResources(resources)
                .recommendedIpRanges(ipRanges)
                .recommendedSchedules(scheduleWindows.stream()
                        .map(window -> PermissionPolicyPresetResponse.ScheduleWindow.builder()
                                .timezone(window.timezone())
                                .days(window.days())
                                .start(window.startTime())
                                .end(window.endTime())
                                .build())
                        .toList())
                .additionalContext(additionalContext)
                .build();
    }

    public static Optional<SecurityPolicyPreset> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(preset -> preset.code.equalsIgnoreCase(code))
                .findFirst();
    }

    public static Set<String> allowedCodes() {
        return Arrays.stream(values())
                .map(SecurityPolicyPreset::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static List<PermissionPolicyPresetResponse> toResponseList() {
        return Arrays.stream(values())
                .map(SecurityPolicyPreset::toResponse)
                .toList();
    }

    public record ScheduleWindow(String timezone, List<String> days, String startTime, String endTime) {
        public ScheduleWindow(String timezone, List<String> days, String startTime, String endTime) {
            this.timezone = timezone != null ? timezone : "UTC";
            this.days = days != null ? List.copyOf(days) : List.of();
            this.startTime = startTime != null ? startTime : "00:00";
            this.endTime = endTime != null ? endTime : "23:59";
        }

        public static ScheduleWindow of(String timezone, List<String> days, String startTime, String endTime) {
            return new ScheduleWindow(timezone, days, startTime, endTime);
        }
    }

    private static final class WorkweekDays {
        static final List<String> BUSINESS_DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
        static final List<String> WEEKENDS = List.of("SATURDAY", "SUNDAY");
        static final List<String> ALL_DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        static final List<String> EXTENDED_DAYS = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
        static final List<String> SEVEN_DAYS = ALL_DAYS;

        private WorkweekDays() {}
    }
}
