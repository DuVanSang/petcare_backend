package com.petcare.backend.service;

import com.petcare.backend.dto.pet.request.CreateBreedRequest;
import com.petcare.backend.dto.pet.request.CreateSpeciesRequest;
import com.petcare.backend.dto.pet.response.BreedResponse;
import com.petcare.backend.dto.pet.response.SpeciesResponse;
import com.petcare.backend.dto.common.PageResponse;

public interface CategoryService {

    PageResponse<SpeciesResponse> getAllSpecies(int page, int size);

    PageResponse<BreedResponse> getBreedsBySpecies(Long speciesId, int page, int size);

    SpeciesResponse createSpecies(CreateSpeciesRequest request);

    BreedResponse createBreed(Long speciesId, CreateBreedRequest request);
}
