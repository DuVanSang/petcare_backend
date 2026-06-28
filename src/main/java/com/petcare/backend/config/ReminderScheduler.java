package com.petcare.backend.config;

import com.petcare.backend.service.ReminderEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.reminder.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ReminderScheduler {
    private final ReminderEngineService reminderEngineService;

    @Scheduled(fixedDelayString = "${app.reminder.custom-worker-delay-ms:60000}")
    public void processCustomReminders() {
        reminderEngineService.processDueCustomReminders();
    }

    @Scheduled(fixedDelayString = "${app.reminder.vaccine-worker-delay-ms:60000}")
    public void processVaccinationReminders() {
        reminderEngineService.processSystemVaccinationReminders();
    }
}
