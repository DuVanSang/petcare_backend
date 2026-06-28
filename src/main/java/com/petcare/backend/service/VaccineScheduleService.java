package com.petcare.backend.service;

import com.petcare.backend.dto.pet.request.VaccinationHistoryRequest;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.User;
import java.util.List;

public interface VaccineScheduleService {
    void generateProposedSchedule(Pet pet, List<VaccinationHistoryRequest> histories);

    List<PetVaccination> confirmPlan(Pet pet, User confirmer, String notes);

    void recalculateAfterCompletion(PetVaccination completedVaccination);
}
