package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.pet.request.CreateBreedRequest;
import com.petcare.backend.dto.pet.request.CreateSpeciesRequest;
import com.petcare.backend.dto.pet.response.BreedResponse;
import com.petcare.backend.dto.pet.response.SpeciesResponse;
import com.petcare.backend.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {
    @Mock CategoryService categoryService;
    private CategoryController controller;
    private MockMvc mockMvc;

    @BeforeEach void setUp() { controller = new CategoryController(categoryService); mockMvc = MockMvcBuilders.standaloneSetup(controller).build(); }

    @Test void allEndpointsDelegateAndCreateEndpointsReturnCreated() {
        PageResponse<SpeciesResponse> speciesPage = PageResponse.from(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));
        PageResponse<BreedResponse> breedPage = PageResponse.from(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));
        SpeciesResponse species = new SpeciesResponse();
        BreedResponse breed = new BreedResponse();
        CreateSpeciesRequest speciesRequest = new CreateSpeciesRequest(); speciesRequest.setName("Dog");
        CreateBreedRequest breedRequest = new CreateBreedRequest(); breedRequest.setName("Poodle");
        when(categoryService.getAllSpecies(0, 20)).thenReturn(speciesPage);
        when(categoryService.getBreedsBySpecies(1L, 0, 20)).thenReturn(breedPage);
        when(categoryService.createSpecies(speciesRequest)).thenReturn(species);
        when(categoryService.createBreed(1L, breedRequest)).thenReturn(breed);

        assertThat(controller.getAllSpecies(0, 20).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getBreeds(1L, 0, 20).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.createSpecies(speciesRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.createBreed(1L, breedRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(categoryService).getAllSpecies(0, 20);
        verify(categoryService).getBreedsBySpecies(1L, 0, 20);
        verify(categoryService).createSpecies(speciesRequest);
        verify(categoryService).createBreed(1L, breedRequest);
    }

    @Test void createSpecies_rejectsNullEmptyAndWhitespaceNamesBeforeService() throws Exception {
        for (String json : java.util.List.of("{}", "{\"name\":\"\"}", "{\"name\":\"   \"}")) {
            mockMvc.perform(post("/api/v1/categories/species").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(categoryService);
    }
}
