package com.petcare.backend.dto.vaccination.response;

import com.petcare.backend.model.PetVaccination;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VaccinationResponse {
    private Long id;
    private Long petId;
    private String petName;
    private Long vaccineTemplateId;
    private String vaccineName;
    private String seriesCode;
    private String targetStage;
    private Integer doseNumber;
    private Integer minimumAgeWeeks;
    private Integer intervalFromPreviousDays;
    private Integer boosterIntervalMonths;
    private String status;
    private String scheduleSource;
    private Boolean scheduleLocked;
    private LocalDate scheduledDate;
    private LocalDate actualDate;
    private String administeredBy;
    private String clinicName;
    private BigDecimal cost;
    private String notes;
    private String medicalProofUrl;
    private LocalDateTime confirmedAt;
    private Long confirmedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private VaccinationSafetyWarningResponse safetyWarning;

    public static VaccinationResponse from(PetVaccination vaccination) {
        VaccinationResponse response = new VaccinationResponse();
        response.setId(vaccination.getId());
        response.setPetId(vaccination.getPet().getId());
        response.setPetName(vaccination.getPet().getName());
        if (vaccination.getVaccineTemplate() != null) {
            response.setVaccineTemplateId(vaccination.getVaccineTemplate().getId());
        }
        response.setVaccineName(vaccination.getVaccineName());
        response.setSeriesCode(vaccination.getSeriesCode());
        response.setTargetStage(vaccination.getTargetStage() != null ? vaccination.getTargetStage().name() : null);
        response.setDoseNumber(vaccination.getDoseNumber());
        response.setMinimumAgeWeeks(vaccination.getMinimumAgeWeeks());
        response.setIntervalFromPreviousDays(vaccination.getIntervalFromPreviousDays());
        response.setBoosterIntervalMonths(vaccination.getBoosterIntervalMonths());
        response.setStatus(vaccination.getStatus() != null ? vaccination.getStatus().name() : null);
        response.setScheduleSource(vaccination.getScheduleSource() != null
                ? vaccination.getScheduleSource().name() : null);
        response.setScheduleLocked(vaccination.getScheduleLocked());
        response.setScheduledDate(vaccination.getScheduledDate());
        response.setActualDate(vaccination.getActualDate());
        response.setAdministeredBy(vaccination.getAdministeredBy());
        response.setClinicName(vaccination.getClinicName());
        response.setCost(vaccination.getCost());
        response.setNotes(vaccination.getNotes());
        response.setMedicalProofUrl(vaccination.getMedicalProofUrl());
        response.setConfirmedAt(vaccination.getConfirmedAt());
        response.setConfirmedBy(vaccination.getConfirmedBy() != null ? vaccination.getConfirmedBy().getId() : null);
        response.setCreatedAt(vaccination.getCreatedAt());
        response.setUpdatedAt(vaccination.getUpdatedAt());
        return response;
    }

    public static VaccinationResponse from(
            PetVaccination vaccination,
            VaccinationSafetyWarningResponse safetyWarning) {
        VaccinationResponse response = from(vaccination);
        response.setSafetyWarning(safetyWarning);
        return response;
    }
}
