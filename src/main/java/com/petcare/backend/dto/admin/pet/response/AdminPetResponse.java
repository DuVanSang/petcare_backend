package com.petcare.backend.dto.admin.pet.response;

import com.petcare.backend.model.Pet;
import com.petcare.backend.util.BreedCategoryHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminPetResponse {
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
    private String gender;
    private LocalDate dateOfBirth;
    private Integer estimatedAgeMonths;
    private BigDecimal currentWeight;
    private String status;
    private String vaccinePlanStatus;
    private long coParentCount;
    private long totalVaccinations;
    private long scheduledVaccinations;
    private long overdueVaccinations;
    private long completedVaccinations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminPetResponse from(
            Pet pet,
            long coParentCount,
            long totalVaccinations,
            long scheduledVaccinations,
            long overdueVaccinations,
            long completedVaccinations
    ) {
        return AdminPetResponse.builder()
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
                .gender(pet.getGender() == null ? null : pet.getGender().name())
                .dateOfBirth(pet.getDateOfBirth())
                .estimatedAgeMonths(pet.getEstimatedAgeMonths())
                .currentWeight(pet.getCurrentWeight())
                .status(pet.getStatus() == null ? null : pet.getStatus().name())
                .vaccinePlanStatus(pet.getVaccinePlanStatus() == null ? null : pet.getVaccinePlanStatus().name())
                .coParentCount(coParentCount)
                .totalVaccinations(totalVaccinations)
                .scheduledVaccinations(scheduledVaccinations)
                .overdueVaccinations(overdueVaccinations)
                .completedVaccinations(completedVaccinations)
                .createdAt(pet.getCreatedAt())
                .updatedAt(pet.getUpdatedAt())
                .build();
    }
}
