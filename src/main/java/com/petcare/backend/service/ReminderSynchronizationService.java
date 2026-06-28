package com.petcare.backend.service;

import com.petcare.backend.model.PetVaccination;
import java.time.LocalDate;

public interface ReminderSynchronizationService {
    void rescheduleVaccinationReminders(PetVaccination vaccination, LocalDate previousDate);

    void cancelVaccinationReminders(PetVaccination vaccination);
}
