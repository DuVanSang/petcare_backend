package com.petcare.backend.dto.reminder.response;

import com.petcare.backend.model.CareReminderLog;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReminderLogResponse {
    private Long id;
    private Instant dueAt;
    private String status;
    private Instant notifiedAt;
    private Instant completedAt;
    private Long completedBy;
    private Instant snoozedUntil;

    public static ReminderLogResponse from(CareReminderLog log) {
        return ReminderLogResponse.builder()
                .id(log.getId())
                .dueAt(log.getDueAt())
                .status(log.getStatus().name())
                .notifiedAt(log.getNotifiedAt())
                .completedAt(log.getCompletedAt())
                .completedBy(log.getCompletedBy() != null ? log.getCompletedBy().getId() : null)
                .snoozedUntil(log.getSnoozedUntil())
                .build();
    }
}
