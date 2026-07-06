package com.petcare.backend.dto.admin.reminder.response;

import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminReminderLogResponse {
    private Long id;
    private Long reminderId;
    private Long petId;
    private String petName;
    private Long createdById;
    private String createdByName;
    private String createdByEmail;
    private Long vaccinationId;
    private String category;
    private String title;
    private String frequency;
    private Boolean reminderActive;
    private Instant dueAt;
    private LocalDate dueDate;
    private String status;
    private Instant notifiedAt;
    private Instant completedAt;
    private Long completedById;
    private String completedByName;
    private Instant snoozedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminReminderLogResponse from(CareReminderLog log) {
        CareReminder reminder = log.getReminder();
        return AdminReminderLogResponse.builder()
                .id(log.getId())
                .reminderId(reminder == null ? null : reminder.getId())
                .petId(reminder == null || reminder.getPet() == null ? null : reminder.getPet().getId())
                .petName(reminder == null || reminder.getPet() == null ? null : reminder.getPet().getName())
                .createdById(reminder == null || reminder.getCreatedBy() == null ? null : reminder.getCreatedBy().getId())
                .createdByName(reminder == null || reminder.getCreatedBy() == null ? null : reminder.getCreatedBy().getFullName())
                .createdByEmail(reminder == null || reminder.getCreatedBy() == null ? null : reminder.getCreatedBy().getEmail())
                .vaccinationId(reminder == null || reminder.getVaccination() == null ? null : reminder.getVaccination().getId())
                .category(reminder == null || reminder.getCategory() == null ? null : reminder.getCategory().name())
                .title(reminder == null ? null : reminder.getTitle())
                .frequency(reminder == null || reminder.getFrequency() == null ? null : reminder.getFrequency().name())
                .reminderActive(reminder == null ? null : reminder.getActive())
                .dueAt(log.getDueAt())
                .dueDate(log.getDueDate())
                .status(log.getStatus() == null ? null : log.getStatus().name())
                .notifiedAt(log.getNotifiedAt())
                .completedAt(log.getCompletedAt())
                .completedById(log.getCompletedBy() == null ? null : log.getCompletedBy().getId())
                .completedByName(log.getCompletedBy() == null ? null : log.getCompletedBy().getFullName())
                .snoozedUntil(log.getSnoozedUntil())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}
