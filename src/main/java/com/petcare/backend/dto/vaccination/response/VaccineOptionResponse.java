package com.petcare.backend.dto.vaccination.response;

import com.petcare.backend.model.VaccineTemplate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VaccineOptionResponse {
    private Long templateId;
    private Long speciesId;
    private String speciesName;
    private String vaccineName;
    private String seriesCode;
    private String targetStage;
    private Integer doseNumber;
    private Integer minimumAgeWeeks;
    private Integer intervalFromPreviousDays;
    private Integer boosterIntervalMonths;
    private Boolean optional;
    private String description;

    public static VaccineOptionResponse from(VaccineTemplate template) {
        return VaccineOptionResponse.builder()
                .templateId(template.getId())
                .speciesId(template.getSpecies() == null ? null : template.getSpecies().getId())
                .speciesName(template.getSpecies() == null ? null : template.getSpecies().getName())
                .vaccineName(template.getVaccineName())
                .seriesCode(template.getSeriesCode())
                .targetStage(template.getTargetStage() == null ? null : template.getTargetStage().name())
                .doseNumber(template.getDoseNumber())
                .minimumAgeWeeks(template.effectiveMinimumAgeWeeks())
                .intervalFromPreviousDays(template.getIntervalFromPreviousDays())
                .boosterIntervalMonths(template.getBoosterIntervalMonths())
                .optional(Boolean.TRUE.equals(template.getOptional()))
                .description(template.getDescription())
                .build();
    }
}
