package com.petcare.backend.service.impl;

import com.petcare.backend.dto.admin.pet.response.AdminPetDetailResponse;
import com.petcare.backend.dto.admin.pet.response.AdminPetResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.vaccination.response.VaccinationResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.service.AdminPetService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminPetServiceImpl implements AdminPetService {
    private static final int MAX_PAGE_SIZE = 100;

    private final PetRepository petRepository;
    private final PetCoParentRepository petCoParentRepository;
    private final PetVaccinationRepository petVaccinationRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminPetResponse> getPets(
            String keyword,
            Long ownerId,
            Long speciesId,
            Pet.PetStatus status,
            Pet.VaccinePlanStatus vaccinePlanStatus,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(validatePage(page), validateSize(size));
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        return PageResponse.from(petRepository
                .searchForAdmin(normalizedKeyword, ownerId, speciesId, status, vaccinePlanStatus, pageable)
                .map(this::toListResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPetDetailResponse getPetDetail(Long petId) {
        Pet pet = getPetOrThrow(petId);
        List<PetCoParent> coParents = petCoParentRepository.findByPetId(petId);

        return AdminPetDetailResponse.from(
                pet,
                coParents,
                petVaccinationRepository.countByPetId(petId),
                countVaccinations(petId, PetVaccination.VaccinationStatus.scheduled),
                countVaccinations(petId, PetVaccination.VaccinationStatus.overdue),
                countVaccinations(petId, PetVaccination.VaccinationStatus.completed)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VaccinationResponse> getPetVaccinations(Long petId, PetVaccination.VaccinationStatus status) {
        getPetOrThrow(petId);

        List<PetVaccination> vaccinations = status == null
                ? petVaccinationRepository.findByPetIdOrderByScheduledDateAsc(petId)
                : petVaccinationRepository.findByPetIdAndStatusOrderByScheduledDateAsc(petId, status);

        return vaccinations.stream()
                .map(VaccinationResponse::from)
                .toList();
    }

    private AdminPetResponse toListResponse(Pet pet) {
        Long petId = pet.getId();
        return AdminPetResponse.from(
                pet,
                petCoParentRepository.findByPetId(petId).size(),
                petVaccinationRepository.countByPetId(petId),
                countVaccinations(petId, PetVaccination.VaccinationStatus.scheduled),
                countVaccinations(petId, PetVaccination.VaccinationStatus.overdue),
                countVaccinations(petId, PetVaccination.VaccinationStatus.completed)
        );
    }

    private long countVaccinations(Long petId, PetVaccination.VaccinationStatus status) {
        return petVaccinationRepository.countByPetIdAndStatus(petId, status);
    }

    private Pet getPetOrThrow(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng"));
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw new BadRequestException("Số trang không được âm");
        }
        return page;
    }

    private int validateSize(int size) {
        if (size <= 0) {
            throw new BadRequestException("Kích thước trang phải lớn hơn 0");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
