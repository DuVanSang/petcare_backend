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
    private Integer doseNumber;
    private String status;
    private LocalDate scheduledDate;
    private LocalDate actualDate;
    private String administeredBy;
    private String clinicName;
    private BigDecimal cost;
    private String notes;
    private String medicalProofUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VaccinationResponse from(PetVaccination vaccination) {
        VaccinationResponse response = new VaccinationResponse();
        response.setId(vaccination.getId());
        response.setPetId(vaccination.getPet().getId());
        response.setPetName(vaccination.getPet().getName());
        if (vaccination.getVaccineTemplate() != null) {
            response.setVaccineTemplateId(vaccination.getVaccineTemplate().getId());
        }
        response.setVaccineName(vaccination.getVaccineName());
        response.setDoseNumber(vaccination.getDoseNumber());
        response.setStatus(vaccination.getStatus() != null ? vaccination.getStatus().name() : null);
        response.setScheduledDate(vaccination.getScheduledDate());
        response.setActualDate(vaccination.getActualDate());
        response.setAdministeredBy(vaccination.getAdministeredBy());
        response.setClinicName(vaccination.getClinicName());
        response.setCost(vaccination.getCost());
        response.setNotes(vaccination.getNotes());
        response.setMedicalProofUrl(vaccination.getMedicalProofUrl());
        response.setCreatedAt(vaccination.getCreatedAt());
        response.setUpdatedAt(vaccination.getUpdatedAt());
        return response;
    }
}
