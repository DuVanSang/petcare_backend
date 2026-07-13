package com.petcare.backend.dto.reminder.response;

import com.petcare.backend.model.CareReminder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReminderResponse {
    private Long id;
    private Long petId;
    private String petName;
    private Long vaccinationId;
    private String category;
    private String status;
    private String title;
    private String notes;
    private LocalDate date;
    private LocalTime time;
    private String timezone;
    private String repeat;
    private Instant nextDueAt;
    private LocalDate endDate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReminderResponse from(CareReminder reminder) {
        return from(reminder, null);
    }

    public static ReminderResponse from(CareReminder reminder, String status) {
        return ReminderResponse.builder()
                .id(reminder.getId())
                .petId(reminder.getPet().getId())
                .petName(reminder.getPet().getName())
                .vaccinationId(reminder.getVaccination() != null ? reminder.getVaccination().getId() : null)
                .category(reminder.getCategory().name())
                .status(status)
                .title(reminder.getTitle())
                .notes(reminder.getNotes())
                .date(reminder.getStartDate())
                .time(reminder.getReminderTime())
                .timezone(reminder.getTimezone())
                .repeat(reminder.getFrequency().name())
                .nextDueAt(reminder.getNextDueAt())
                .endDate(reminder.getEndDate())
                .active(reminder.getActive())
                .createdAt(reminder.getCreatedAt())
                .updatedAt(reminder.getUpdatedAt())
                .build();
    }
}
