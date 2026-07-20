package com.petcare.backend.service;

import com.petcare.backend.dto.vaccination.response.VaccinationSafetyWarningResponse;
import com.petcare.backend.model.PetVaccination;

public interface VaccinationSafetyService {
    VaccinationSafetyWarningResponse evaluate(PetVaccination vaccination);
}
