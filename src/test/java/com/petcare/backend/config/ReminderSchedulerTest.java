package com.petcare.backend.config;

import static org.mockito.Mockito.verify;

import com.petcare.backend.service.ReminderEngineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {
    @Mock private ReminderEngineService engine;

    @Test
    void scheduledWorkers_DelegateToTheirMatchingEngineOperations() {
        ReminderScheduler scheduler = new ReminderScheduler(engine);
        scheduler.processCustomReminders();
        scheduler.processVaccinationReminders();

        verify(engine).processDueCustomReminders();
        verify(engine).processSystemVaccinationReminders();
    }
}
