package com.petcare.backend.dto.health.response;

import com.petcare.backend.model.WeightLog;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class WeightLogResponse {

    private Long id;
    private Long petId;
    private BigDecimal weight;
    private LocalDate loggedDate;

    public static WeightLogResponse from(WeightLog weightLog) {
        return WeightLogResponse.builder()
                .id(weightLog.getId())
                .petId(weightLog.getPet().getId())
                .weight(weightLog.getWeight())
                .loggedDate(weightLog.getLoggedDate())
                .build();
    }
}
