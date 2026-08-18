package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.VaccinationReminderLog;
import com.petcare.backend.repository.CareReminderLogRepository;
import com.petcare.backend.repository.CareReminderRepository;
import com.petcare.backend.repository.VaccinationReminderLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderSynchronizationServiceImplTest {
    @Mock CareReminderRepository reminders;
    @Mock CareReminderLogRepository reminderLogs;
    @Mock VaccinationReminderLogRepository vaccinationLogs;
    @Mock ReminderScheduleCalculator calculator;
    private ReminderSynchronizationServiceImpl service;

    @BeforeEach void setUp() { service = new ReminderSynchronizationServiceImpl(reminders, reminderLogs, vaccinationLogs, calculator); }

    private PetVaccination vaccination() {
        PetVaccination vaccination = new PetVaccination(); vaccination.setId(1L); vaccination.setScheduledDate(LocalDate.of(2025, 1, 10)); return vaccination;
    }
    private CareReminder reminder(long id, Long offset) {
        CareReminder reminder = new CareReminder(); reminder.setId(id); reminder.setActive(true); reminder.setTimezone("Asia/Ho_Chi_Minh"); reminder.setVaccinationOffsetMinutes(offset); return reminder;
    }
    private CareReminderLog log(CareReminderLog.ReminderLogStatus status) {
        CareReminderLog log = new CareReminderLog(); log.setStatus(status); log.setSnoozedUntil(Instant.now()); return log;
    }

    @Test void reschedule_skipsEmptyOrOffsetlessReminderAndUpdatesOutstandingLogs() {
        PetVaccination vaccination = vaccination();
        VaccinationReminderLog systemLog = new VaccinationReminderLog(); systemLog.setStatus(VaccinationReminderLog.VaccinationReminderStatus.pending);
        CareReminder noOutstanding = reminder(2L, 30L);
        CareReminder noOffset = reminder(3L, null);
        CareReminder valid = reminder(4L, 60L);
        CareReminderLog pending = log(CareReminderLog.ReminderLogStatus.pending);
        CareReminderLog snoozed = log(CareReminderLog.ReminderLogStatus.snoozed);
        when(vaccinationLogs.findByVaccinationIdAndStatus(1L, VaccinationReminderLog.VaccinationReminderStatus.pending)).thenReturn(List.of(systemLog));
        when(reminders.findByVaccinationIdAndActiveTrue(1L)).thenReturn(List.of(noOutstanding, noOffset, valid));
        when(reminderLogs.findByReminderIdAndStatusIn(eq(2L), anySet())).thenReturn(List.of());
        when(reminderLogs.findByReminderIdAndStatusIn(eq(3L), anySet())).thenReturn(List.of(pending));
        when(reminderLogs.findByReminderIdAndStatusIn(eq(4L), anySet())).thenReturn(List.of(pending, snoozed));
        LocalDate expectedDate = LocalDate.of(2025, 1, 11);
        when(calculator.toLocalDate(any(Instant.class), eq("Asia/Ho_Chi_Minh"))).thenReturn(expectedDate);

        service.rescheduleVaccinationReminders(vaccination, LocalDate.of(2025, 1, 1));

        assertThat(systemLog.getStatus()).isEqualTo(VaccinationReminderLog.VaccinationReminderStatus.cancelled);
        assertThat(valid.getStartDate()).isEqualTo(expectedDate); assertThat(valid.getNextDueDate()).isEqualTo(expectedDate);
        assertThat(valid.getNextDueAt()).isNotNull(); assertThat(valid.getReminderTime()).isNotNull();
        assertThat(pending.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.pending);
        assertThat(snoozed.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.pending);
        assertThat(snoozed.getSnoozedUntil()).isNull();
        verify(reminders).save(valid); verify(reminders, never()).save(noOutstanding); verify(reminders, never()).save(noOffset);
        verify(reminderLogs, times(2)).save(any(CareReminderLog.class)); verify(vaccinationLogs).save(systemLog);
    }

    @Test void reschedule_withNoSystemLogsAndNoRemindersIsEarlyReturnWithoutSave() {
        PetVaccination vaccination = vaccination();
        when(vaccinationLogs.findByVaccinationIdAndStatus(1L, VaccinationReminderLog.VaccinationReminderStatus.pending)).thenReturn(List.of());
        when(reminders.findByVaccinationIdAndActiveTrue(1L)).thenReturn(List.of());
        service.rescheduleVaccinationReminders(vaccination, null);
        verifyNoInteractions(reminderLogs, calculator);
        verify(reminders, never()).save(any()); verify(vaccinationLogs, never()).save(any());
    }

    @Test void cancel_deactivatesRemindersCancelsOutstandingAndSystemLogs() {
        PetVaccination vaccination = vaccination();
        VaccinationReminderLog systemLog = new VaccinationReminderLog(); systemLog.setStatus(VaccinationReminderLog.VaccinationReminderStatus.pending);
        CareReminder reminder = reminder(2L, 30L); reminder.setNextDueAt(Instant.now());
        CareReminderLog pending = log(CareReminderLog.ReminderLogStatus.pending);
        CareReminderLog snoozed = log(CareReminderLog.ReminderLogStatus.snoozed);
        when(vaccinationLogs.findByVaccinationIdAndStatus(1L, VaccinationReminderLog.VaccinationReminderStatus.pending)).thenReturn(List.of(systemLog));
        when(reminders.findByVaccinationIdAndActiveTrue(1L)).thenReturn(List.of(reminder));
        when(reminderLogs.findByReminderIdAndStatusIn(eq(2L), anySet())).thenReturn(List.of(pending, snoozed));

        service.cancelVaccinationReminders(vaccination);

        assertThat(reminder.getActive()).isFalse(); assertThat(reminder.getNextDueAt()).isNull();
        assertThat(systemLog.getStatus()).isEqualTo(VaccinationReminderLog.VaccinationReminderStatus.cancelled);
        assertThat(pending.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.cancelled);
        assertThat(snoozed.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.cancelled);
        verify(reminders).save(reminder); verify(reminderLogs, times(2)).save(any(CareReminderLog.class)); verify(vaccinationLogs).save(systemLog);
    }
}
