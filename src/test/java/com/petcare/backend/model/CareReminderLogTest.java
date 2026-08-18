package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CareReminderLogTest {
    @Test
    void accessorsDefaultStatusRelationsAndLifecycle_PreserveCompletionAndSnoozeData() {
        CareReminderLog log = new CareReminderLog(); CareReminder reminder = new CareReminder(); User user = new User(); user.setId(1L);
        assertThat(log.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.pending);
        log.setId(2L); log.setReminder(reminder); log.setDueAt(Instant.parse("2026-01-01T00:00:00Z")); log.setDueDate(LocalDate.of(2026, 1, 1));
        log.setStatus(CareReminderLog.ReminderLogStatus.completed); log.setNotifiedAt(null); log.setCompletedAt(Instant.parse("2026-01-01T01:00:00Z"));
        log.setCompletedBy(user); log.setSnoozedUntil(null);
        assertThat(log.getReminder()).isSameAs(reminder); assertThat(log.getCompletedBy()).isSameAs(user); assertThat(log.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.completed);
        log.prePersist(); LocalDateTime beforeUpdate = log.getUpdatedAt(); assertThat(log.getCreatedAt()).isNotNull();
        log.preUpdate(); assertThat(log.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }
}
