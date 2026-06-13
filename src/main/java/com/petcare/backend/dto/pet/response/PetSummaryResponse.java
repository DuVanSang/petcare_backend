package com.petcare.backend.dto.pet.response;

import com.petcare.backend.model.Pet;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PetSummaryResponse {

    private Long id;
    private String name;
    private String avatarUrl;
    private String speciesName;
    private String breedName;
    private String gender;
    private String status;
    private String myRole;
    private LocalDateTime createdAt;

    public static PetSummaryResponse from(Pet pet, String myRole) {
        PetSummaryResponse dto = new PetSummaryResponse();
        dto.setId(pet.getId());
        dto.setName(pet.getName());
        dto.setAvatarUrl(pet.getAvatarUrl());
        dto.setSpeciesName(pet.getSpecies() != null ? pet.getSpecies().getName() : null);
        dto.setBreedName(pet.getBreed() != null ? pet.getBreed().getName() : null);
        dto.setGender(pet.getGender() != null ? pet.getGender().name() : null);
        dto.setStatus(pet.getStatus() != null ? pet.getStatus().name() : null);
        dto.setMyRole(myRole);
        dto.setCreatedAt(pet.getCreatedAt());
        return dto;
    }
}
