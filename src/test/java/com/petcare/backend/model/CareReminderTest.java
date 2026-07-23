package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class CareReminderTest {
    @Test
    void accessorsDefaultsEnumsRelationshipsAndLifecycle_PreserveReminderConfiguration() {
        CareReminder reminder = new CareReminder(); Pet pet = new Pet(); pet.setId(1L); User creator = new User(); creator.setId(2L);
        reminder.setId(3L); reminder.setPet(pet); reminder.setCreatedBy(creator); reminder.setVaccination(null);
        reminder.setCategory(CareReminder.ReminderCategory.medication); reminder.setTitle("Medicine"); reminder.setNotes("");
        reminder.setStartDate(LocalDate.of(2026, 1, 1)); reminder.setReminderTime(LocalTime.NOON); reminder.setTimezone("UTC");
        reminder.setFrequency(CareReminder.ReminderFrequency.weekly); reminder.setNextDueAt(Instant.parse("2026-01-02T12:00:00Z"));
        reminder.setNextDueDate(LocalDate.of(2026, 1, 2)); reminder.setEndDate(null); reminder.setIntervalValue(0);
        reminder.setBeforeDurationMinutes(-1); reminder.setVaccinationOffsetMinutes(null); reminder.setActive(false);
        assertThat(reminder.getIntervalValue()).isZero(); assertThat(reminder.getBeforeDurationMinutes()).isEqualTo(-1);
        assertThat(reminder.getPet()).isSameAs(pet); assertThat(reminder.getCreatedBy()).isSameAs(creator); assertThat(reminder.getActive()).isFalse();
        reminder.prePersist(); LocalDateTime beforeUpdate = reminder.getUpdatedAt();
        assertThat(reminder.getCreatedAt()).isNotNull(); reminder.preUpdate(); assertThat(reminder.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }
}
