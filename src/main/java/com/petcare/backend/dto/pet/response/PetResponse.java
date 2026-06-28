package com.petcare.backend.dto.pet.response;

import com.petcare.backend.model.Pet;
import com.petcare.backend.util.BreedCategoryHelper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PetResponse {

    private Long id;
    private String name;
    private String avatarUrl;

    // Owner info
    private Long ownerId;
    private String ownerName;

    // Species & Breed
    private Long speciesId;
    private String speciesName;
    private Long breedId;
    private String breedName;

    @Schema(description = "Giống tự nhập (khi chọn Khác trên dropdown)")
    private String customBreedName;

    // Bio
    private String gender;
    private LocalDate dateOfBirth;
    private Integer estimatedAgeMonths;
    private BigDecimal currentWeight;
    private String colorFeatures;
    private String spayedStatus;
    private String status;
    private String vaccinePlanStatus;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Role của người đang xem (owner / editor / viewer)
    private String myRole;

    public static PetResponse from(Pet pet, String myRole) {
        PetResponse dto = new PetResponse();
        dto.setId(pet.getId());
        dto.setName(pet.getName());
        dto.setAvatarUrl(pet.getAvatarUrl());

        dto.setOwnerId(pet.getOwner().getId());
        dto.setOwnerName(pet.getOwner().getFullName());

        if (pet.getSpecies() != null) {
            dto.setSpeciesId(pet.getSpecies().getId());
            dto.setSpeciesName(pet.getSpecies().getName());
        }
        if (pet.getBreed() != null) {
            dto.setBreedId(pet.getBreed().getId());
            dto.setBreedName(BreedCategoryHelper.displayBreedName(pet.getBreed(), pet.getCustomBreedName()));
            dto.setCustomBreedName(pet.getCustomBreedName());
        }

        dto.setGender(pet.getGender() != null ? pet.getGender().name() : null);
        dto.setDateOfBirth(pet.getDateOfBirth());
        dto.setEstimatedAgeMonths(pet.getEstimatedAgeMonths());
        dto.setCurrentWeight(pet.getCurrentWeight());
        dto.setColorFeatures(pet.getColorFeatures());
        dto.setSpayedStatus(pet.getSpayedStatus() != null ? pet.getSpayedStatus().name() : null);
        dto.setStatus(pet.getStatus() != null ? pet.getStatus().name() : null);
        dto.setVaccinePlanStatus(pet.getVaccinePlanStatus() != null ? pet.getVaccinePlanStatus().name() : null);
        dto.setNotes(pet.getNotes());
        dto.setCreatedAt(pet.getCreatedAt());
        dto.setUpdatedAt(pet.getUpdatedAt());
        dto.setMyRole(myRole);

        return dto;
    }
}
