package com.petcare.backend.dto.admin.pet.response;

import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.util.BreedCategoryHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminPetDetailResponse {
    private Long id;
    private String name;
    private String avatarUrl;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private Long speciesId;
    private String speciesName;
    private Long breedId;
    private String breedName;
    private String customBreedName;
    private String gender;
    private LocalDate dateOfBirth;
    private Integer estimatedAgeMonths;
    private BigDecimal currentWeight;
    private String colorFeatures;
    private String spayedStatus;
    private String status;
    private String vaccinePlanStatus;
    private String notes;
    private long coParentCount;
    private long totalVaccinations;
    private long scheduledVaccinations;
    private long overdueVaccinations;
    private long completedVaccinations;
    private List<AdminPetCoParentResponse> coParents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminPetDetailResponse from(
            Pet pet,
            List<PetCoParent> coParents,
            long totalVaccinations,
            long scheduledVaccinations,
            long overdueVaccinations,
            long completedVaccinations
    ) {
        List<AdminPetCoParentResponse> coParentResponses = coParents.stream()
                .map(AdminPetCoParentResponse::from)
                .toList();

        return AdminPetDetailResponse.builder()
                .id(pet.getId())
                .name(pet.getName())
                .avatarUrl(pet.getAvatarUrl())
                .ownerId(pet.getOwner() == null ? null : pet.getOwner().getId())
                .ownerName(pet.getOwner() == null ? null : pet.getOwner().getFullName())
                .ownerEmail(pet.getOwner() == null ? null : pet.getOwner().getEmail())
                .speciesId(pet.getSpecies() == null ? null : pet.getSpecies().getId())
                .speciesName(pet.getSpecies() == null ? null : pet.getSpecies().getName())
                .breedId(pet.getBreed() == null ? null : pet.getBreed().getId())
                .breedName(pet.getBreed() == null
                        ? null
                        : BreedCategoryHelper.displayBreedName(pet.getBreed(), pet.getCustomBreedName()))
                .customBreedName(pet.getCustomBreedName())
                .gender(pet.getGender() == null ? null : pet.getGender().name())
                .dateOfBirth(pet.getDateOfBirth())
                .estimatedAgeMonths(pet.getEstimatedAgeMonths())
                .currentWeight(pet.getCurrentWeight())
                .colorFeatures(pet.getColorFeatures())
                .spayedStatus(pet.getSpayedStatus() == null ? null : pet.getSpayedStatus().name())
                .status(pet.getStatus() == null ? null : pet.getStatus().name())
                .vaccinePlanStatus(pet.getVaccinePlanStatus() == null ? null : pet.getVaccinePlanStatus().name())
                .notes(pet.getNotes())
                .coParentCount(coParentResponses.size())
                .totalVaccinations(totalVaccinations)
                .scheduledVaccinations(scheduledVaccinations)
                .overdueVaccinations(overdueVaccinations)
                .completedVaccinations(completedVaccinations)
                .coParents(coParentResponses)
                .createdAt(pet.getCreatedAt())
                .updatedAt(pet.getUpdatedAt())
                .build();
    }
}
