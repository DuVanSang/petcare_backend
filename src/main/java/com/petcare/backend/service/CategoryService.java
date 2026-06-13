package com.petcare.backend.service;

import com.petcare.backend.dto.pet.response.BreedResponse;
import com.petcare.backend.dto.pet.response.SpeciesResponse;

import java.util.List;

public interface CategoryService {

    List<SpeciesResponse> getAllSpecies();

    List<BreedResponse> getBreedsBySpecies(Long speciesId);
}
