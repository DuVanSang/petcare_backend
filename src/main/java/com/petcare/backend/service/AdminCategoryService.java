package com.petcare.backend.service;

import com.petcare.backend.dto.admin.category.request.AdminCreateBreedRequest;
import com.petcare.backend.dto.admin.category.request.AdminCreateSpeciesRequest;
import com.petcare.backend.dto.admin.category.request.AdminUpdateBreedRequest;
import com.petcare.backend.dto.admin.category.request.AdminUpdateSpeciesRequest;
import com.petcare.backend.dto.admin.category.response.AdminBreedResponse;
import com.petcare.backend.dto.admin.category.response.AdminSpeciesResponse;
import com.petcare.backend.dto.common.PageResponse;

public interface AdminCategoryService {
    PageResponse<AdminSpeciesResponse> getSpecies(String keyword, Boolean active, int page, int size);

    AdminSpeciesResponse getSpeciesDetail(Long speciesId);

    AdminSpeciesResponse createSpecies(AdminCreateSpeciesRequest request);

    AdminSpeciesResponse updateSpecies(Long speciesId, AdminUpdateSpeciesRequest request);

    PageResponse<AdminBreedResponse> getBreeds(Long speciesId, String keyword, Boolean active, int page, int size);

    AdminBreedResponse getBreedDetail(Long breedId);

    AdminBreedResponse createBreed(AdminCreateBreedRequest request);

    AdminBreedResponse updateBreed(Long breedId, AdminUpdateBreedRequest request);
}
