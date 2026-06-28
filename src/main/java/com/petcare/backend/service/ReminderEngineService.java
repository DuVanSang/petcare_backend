package com.petcare.backend.service;

public interface ReminderEngineService {
    void processDueCustomReminders();

    void processSystemVaccinationReminders();
}
