package com.petcare.backend.dto.pet.response;

import com.petcare.backend.model.Breed;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BreedResponse {

    private Long id;
    private Long speciesId;
    private String name;

    public static BreedResponse from(Breed breed) {
        BreedResponse dto = new BreedResponse();
        dto.setId(breed.getId());
        dto.setSpeciesId(breed.getSpecies().getId());
        dto.setName(breed.getName());
        return dto;
    }
}
