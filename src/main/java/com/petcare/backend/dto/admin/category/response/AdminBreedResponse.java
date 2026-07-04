package com.petcare.backend.dto.admin.category.response;

import com.petcare.backend.model.Breed;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminBreedResponse {
    private Long id;
    private Long speciesId;
    private String speciesName;
    private String name;
    private Boolean active;

    public static AdminBreedResponse from(Breed breed) {
        return AdminBreedResponse.builder()
                .id(breed.getId())
                .speciesId(breed.getSpecies().getId())
                .speciesName(breed.getSpecies().getName())
                .name(breed.getName())
                .active(breed.getActive())
                .build();
    }
}
