package com.petcare.backend.dto.health.response;

import com.petcare.backend.model.HealthLog;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class HealthLogResponse {

    private Long id;
    private Long petId;
    private LocalDate loggedDate;
    private HealthLog.Appetite appetite;
    private String notes;
    private BigDecimal weight;
    private BigDecimal currentWeight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static HealthLogResponse from(HealthLog healthLog, BigDecimal weight, BigDecimal currentWeight) {
        return HealthLogResponse.builder()
                .id(healthLog.getId())
                .petId(healthLog.getPet().getId())
                .loggedDate(healthLog.getLoggedDate())
                .appetite(healthLog.getAppetite())
                .notes(healthLog.getTreatmentNotes())
                .weight(weight)
                .currentWeight(currentWeight)
                .createdAt(healthLog.getCreatedAt())
                .updatedAt(healthLog.getUpdatedAt())
                .build();
    }
}
