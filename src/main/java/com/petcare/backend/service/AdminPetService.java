package com.petcare.backend.service;

import com.petcare.backend.dto.admin.pet.response.AdminPetDetailResponse;
import com.petcare.backend.dto.admin.pet.response.AdminPetResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.vaccination.response.VaccinationResponse;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import java.util.List;

public interface AdminPetService {
    PageResponse<AdminPetResponse> getPets(
            String keyword,
            Long ownerId,
            Long speciesId,
            Pet.PetStatus status,
            Pet.VaccinePlanStatus vaccinePlanStatus,
            int page,
            int size
    );

    AdminPetDetailResponse getPetDetail(Long petId);

    List<VaccinationResponse> getPetVaccinations(Long petId, PetVaccination.VaccinationStatus status);
}
