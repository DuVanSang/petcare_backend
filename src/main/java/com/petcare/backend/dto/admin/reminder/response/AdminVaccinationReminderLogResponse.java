package com.petcare.backend.dto.admin.reminder.response;

import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.VaccinationReminderLog;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminVaccinationReminderLogResponse {
    private Long id;
    private Long vaccinationId;
    private Long petId;
    private String petName;
    private String vaccineName;
    private String vaccinationStatus;
    private LocalDate vaccinationScheduledDate;
    private Long userId;
    private String userName;
    private String userEmail;
    private String stage;
    private Instant scheduledAt;
    private Instant notifiedAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminVaccinationReminderLogResponse from(VaccinationReminderLog log) {
        PetVaccination vaccination = log.getVaccination();
        return AdminVaccinationReminderLogResponse.builder()
                .id(log.getId())
                .vaccinationId(vaccination == null ? null : vaccination.getId())
                .petId(vaccination == null || vaccination.getPet() == null ? null : vaccination.getPet().getId())
                .petName(vaccination == null || vaccination.getPet() == null ? null : vaccination.getPet().getName())
                .vaccineName(vaccination == null ? null : vaccination.getVaccineName())
                .vaccinationStatus(vaccination == null || vaccination.getStatus() == null
                        ? null
                        : vaccination.getStatus().name())
                .vaccinationScheduledDate(vaccination == null ? null : vaccination.getScheduledDate())
                .userId(log.getUser() == null ? null : log.getUser().getId())
                .userName(log.getUser() == null ? null : log.getUser().getFullName())
                .userEmail(log.getUser() == null ? null : log.getUser().getEmail())
                .stage(log.getStage() == null ? null : log.getStage().name())
                .scheduledAt(log.getScheduledAt())
                .notifiedAt(log.getNotifiedAt())
                .status(log.getStatus() == null ? null : log.getStatus().name())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}
