package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.petcare.backend.dto.admin.category.request.*;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.service.AdminCategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminCategoryControllerTest {
    @Mock private AdminCategoryService service;
    @InjectMocks private AdminCategoryController controller;

    @Test
    void speciesEndpoints_ReturnExpectedResponsesAndDelegateArguments() {
        assertOk(controller.getSpecies("dog", true, 0, 1));
        assertOk(controller.getSpeciesDetail(1L));
        assertThat(controller.createSpecies(new AdminCreateSpeciesRequest()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertOk(controller.updateSpecies(1L, new AdminUpdateSpeciesRequest()));
        verify(service).getSpecies("dog", true, 0, 1);
        verify(service).getSpeciesDetail(1L);
        verify(service).createSpecies(org.mockito.ArgumentMatchers.any(AdminCreateSpeciesRequest.class));
        verify(service).updateSpecies(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(AdminUpdateSpeciesRequest.class));
    }

    @Test
    void breedEndpoints_ReturnExpectedResponsesAndDelegateArguments() {
        assertOk(controller.getBreeds(2L, "poodle", false, 3, 20));
        assertOk(controller.getBreedDetail(3L));
        assertThat(controller.createBreed(new AdminCreateBreedRequest()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertOk(controller.updateBreed(3L, new AdminUpdateBreedRequest()));
        verify(service).getBreeds(2L, "poodle", false, 3, 20);
        verify(service).getBreedDetail(3L);
        verify(service).createBreed(org.mockito.ArgumentMatchers.any(AdminCreateBreedRequest.class));
        verify(service).updateBreed(org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.any(AdminUpdateBreedRequest.class));
    }

    private void assertOk(ResponseEntity<? extends ApiResponse<?>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
