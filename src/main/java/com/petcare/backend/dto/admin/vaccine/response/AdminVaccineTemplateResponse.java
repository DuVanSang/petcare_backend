package com.petcare.backend.dto.admin.vaccine.response;

import com.petcare.backend.model.VaccineTemplate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminVaccineTemplateResponse {
    private Long id;
    private Long speciesId;
    private String speciesName;
    private String vaccineName;
    private String seriesCode;
    private VaccineTemplate.TargetStage targetStage;
    private Integer doseNumber;
    private Integer recommendedAgeWeeks;
    private Integer minimumAgeWeeks;
    private Integer intervalFromPreviousDays;
    private Integer boosterIntervalMonths;
    private Boolean optional;
    private Boolean active;
    private String description;

    public static AdminVaccineTemplateResponse from(VaccineTemplate template) {
        return AdminVaccineTemplateResponse.builder()
                .id(template.getId())
                .speciesId(template.getSpecies().getId())
                .speciesName(template.getSpecies().getName())
                .vaccineName(template.getVaccineName())
                .seriesCode(template.getSeriesCode())
                .targetStage(template.getTargetStage())
                .doseNumber(template.getDoseNumber())
                .recommendedAgeWeeks(template.getRecommendedAgeWeeks())
                .minimumAgeWeeks(template.getMinimumAgeWeeks())
                .intervalFromPreviousDays(template.getIntervalFromPreviousDays())
                .boosterIntervalMonths(template.getBoosterIntervalMonths())
                .optional(template.getOptional())
                .active(template.getActive())
                .description(template.getDescription())
                .build();
    }
}
