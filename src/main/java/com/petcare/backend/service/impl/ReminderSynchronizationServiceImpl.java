package com.petcare.backend.service.impl;

import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.VaccinationReminderLog;
import com.petcare.backend.repository.CareReminderLogRepository;
import com.petcare.backend.repository.CareReminderRepository;
import com.petcare.backend.repository.VaccinationReminderLogRepository;
import com.petcare.backend.service.ReminderSynchronizationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderSynchronizationServiceImpl implements ReminderSynchronizationService {
    private static final Set<CareReminderLog.ReminderLogStatus> OUTSTANDING_STATUSES = Set.of(
            CareReminderLog.ReminderLogStatus.pending,
            CareReminderLog.ReminderLogStatus.snoozed
    );

    private final CareReminderRepository reminderRepository;
    private final CareReminderLogRepository reminderLogRepository;
    private final VaccinationReminderLogRepository vaccinationLogRepository;
    private final ReminderScheduleCalculator scheduleCalculator;

    @Override
    @Transactional
    public void rescheduleVaccinationReminders(PetVaccination vaccination, LocalDate previousDate) {
        cancelPendingSystemLogs(vaccination.getId());

        for (CareReminder reminder : reminderRepository.findByVaccinationIdAndActiveTrue(vaccination.getId())) {
            List<CareReminderLog> outstanding = reminderLogRepository.findByReminderIdAndStatusIn(
                    reminder.getId(), OUTSTANDING_STATUSES
            );
            if (outstanding.isEmpty() || reminder.getVaccinationOffsetMinutes() == null) {
                continue;
            }

            ZoneId zoneId = ZoneId.of(reminder.getTimezone());
            Instant newDueAt = vaccination.getScheduledDate()
                    .atStartOfDay(zoneId)
                    .plusMinutes(reminder.getVaccinationOffsetMinutes())
                    .toInstant();
            LocalDate newDate = scheduleCalculator.toLocalDate(newDueAt, reminder.getTimezone());
            reminder.setStartDate(newDate);
            reminder.setReminderTime(newDueAt.atZone(zoneId).toLocalTime());
            reminder.setNextDueAt(newDueAt);
            reminder.setNextDueDate(newDate);
            reminderRepository.save(reminder);

            for (CareReminderLog log : outstanding) {
                log.setDueAt(newDueAt);
                log.setDueDate(newDate);
                log.setSnoozedUntil(null);
                log.setStatus(CareReminderLog.ReminderLogStatus.pending);
                reminderLogRepository.save(log);
            }
        }
    }

    @Override
    @Transactional
    public void cancelVaccinationReminders(PetVaccination vaccination) {
        cancelPendingSystemLogs(vaccination.getId());
        for (CareReminder reminder : reminderRepository.findByVaccinationIdAndActiveTrue(vaccination.getId())) {
            reminder.setActive(false);
            reminder.setNextDueAt(null);
            reminderRepository.save(reminder);
            reminderLogRepository.findByReminderIdAndStatusIn(
                    reminder.getId(), OUTSTANDING_STATUSES
            ).forEach(log -> {
                log.setStatus(CareReminderLog.ReminderLogStatus.cancelled);
                reminderLogRepository.save(log);
            });
        }
    }

    private void cancelPendingSystemLogs(Long vaccinationId) {
        vaccinationLogRepository.findByVaccinationIdAndStatus(
                vaccinationId,
                VaccinationReminderLog.VaccinationReminderStatus.pending
        ).forEach(log -> {
            log.setStatus(VaccinationReminderLog.VaccinationReminderStatus.cancelled);
            vaccinationLogRepository.save(log);
        });
    }
}
