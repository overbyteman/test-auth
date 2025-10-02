package com.seccreto.service.auth.model.usage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyUserUsageId implements Serializable {
    private LocalDate usageDate;
    private UUID userId;
    private UUID tenantId;
}
