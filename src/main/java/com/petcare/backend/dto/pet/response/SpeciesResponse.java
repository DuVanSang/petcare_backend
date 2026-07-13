package com.petcare.backend.dto.pet.response;

import com.petcare.backend.model.Species;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class SpeciesResponse {

    private Long id;
    private String name;
    private String iconUrl;
    private Boolean active;
    private List<BreedResponse> breeds;

    public static SpeciesResponse from(Species species) {
        SpeciesResponse dto = new SpeciesResponse();
        dto.setId(species.getId());
        dto.setName(species.getName());
        dto.setIconUrl(species.getIconUrl());
        dto.setActive(species.getActive());
        return dto;
    }

    public static SpeciesResponse withBreeds(Species species) {
        SpeciesResponse dto = from(species);
        dto.setBreeds(
                species.getBreeds().stream()
                        .filter(breed -> Boolean.TRUE.equals(breed.getActive()))
                        .map(BreedResponse::from)
                        .collect(Collectors.toList())
        );
        return dto;
    }
}
