package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.petcare.backend.dto.pet.request.CreateBreedRequest;
import com.petcare.backend.dto.pet.request.CreateSpeciesRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Breed;
import com.petcare.backend.model.Species;
import com.petcare.backend.repository.BreedRepository;
import com.petcare.backend.repository.SpeciesRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock SpeciesRepository speciesRepository;
    @Mock BreedRepository breedRepository;
    private CategoryServiceImpl service;

    @BeforeEach void setUp() { service = new CategoryServiceImpl(speciesRepository, breedRepository); }

    private Species species(long id, String name) {
        Species species = new Species(); species.setId(id); species.setName(name); species.setActive(true); return species;
    }

    @Test void getAllSpecies_returnsOnlyActiveBreedMappingsAndCapsPageSize() {
        Species dog = species(1L, "Dog");
        Breed active = new Breed(); active.setId(2L); active.setName("Poodle"); active.setSpecies(dog); active.setActive(true);
        Breed inactive = new Breed(); inactive.setId(3L); inactive.setName("Old"); inactive.setSpecies(dog); inactive.setActive(false);
        dog.setBreeds(List.of(active, inactive));
        when(speciesRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(dog)));

        var response = service.getAllSpecies(0, 99);

        assertThat(response.getContent()).singleElement().satisfies(result -> {
            assertThat(result.getName()).isEqualTo("Dog");
            assertThat(result.getBreeds()).extracting("name").containsExactly("Poodle");
        });
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(speciesRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    @Test void paginationRejectsNegativePageAndNonPositiveSize() {
        assertThatThrownBy(() -> service.getAllSpecies(-1, 1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getAllSpecies(0, 0)).isInstanceOf(BadRequestException.class);
        when(speciesRepository.existsById(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.getBreedsBySpecies(1L, -1, 1)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getBreedsBySpecies(1L, 0, 0)).isInstanceOf(BadRequestException.class);
    }

    @Test void getBreedsBySpecies_handlesMissingSpeciesAndReturnsActiveBreedPage() {
        assertThatThrownBy(() -> service.getBreedsBySpecies(99L, 0, 20)).isInstanceOf(BadRequestException.class);
        Species dog = species(1L, "Dog");
        Breed poodle = new Breed(); poodle.setId(2L); poodle.setSpecies(dog); poodle.setName("Poodle"); poodle.setActive(true);
        when(speciesRepository.existsById(1L)).thenReturn(true);
        when(breedRepository.findBySpeciesIdAndActiveTrue(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(poodle)));
        assertThat(service.getBreedsBySpecies(1L, 0, 20).getContent()).extracting("name").containsExactly("Poodle");
    }

    @Test void createSpecies_trimsOptionalIconCreatesOtherBreedAndRejectsDuplicate() {
        CreateSpeciesRequest request = new CreateSpeciesRequest(); request.setName("  Dog  "); request.setIconUrl("  icon.png  ");
        when(speciesRepository.save(any(Species.class))).thenAnswer(invocation -> { Species value = invocation.getArgument(0); value.setId(1L); return value; });
        when(breedRepository.save(any(Breed.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.createSpecies(request);
        assertThat(response.getName()).isEqualTo("Dog");
        assertThat(response.getIconUrl()).isEqualTo("icon.png");
        ArgumentCaptor<Breed> other = ArgumentCaptor.forClass(Breed.class);
        verify(breedRepository).save(other.capture());
        assertThat(other.getValue().getName()).isEqualTo("Khác");

        when(speciesRepository.existsByNameIgnoreCase("Dog")).thenReturn(true);
        assertThatThrownBy(() -> service.createSpecies(request)).isInstanceOf(BadRequestException.class);
    }

    @Test void createSpecies_acceptsBlankOptionalIcon() {
        CreateSpeciesRequest request = new CreateSpeciesRequest(); request.setName("Cat"); request.setIconUrl("   ");
        when(speciesRepository.save(any(Species.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.createSpecies(request);
        ArgumentCaptor<Species> saved = ArgumentCaptor.forClass(Species.class);
        verify(speciesRepository).save(saved.capture());
        assertThat(saved.getValue().getIconUrl()).isNull();
    }

    @Test void createBreed_handlesMissingSpeciesDuplicateAndSuccess() {
        CreateBreedRequest request = new CreateBreedRequest(); request.setName("  Poodle  ");
        when(speciesRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createBreed(1L, request)).isInstanceOf(BadRequestException.class);

        Species dog = species(1L, "Dog");
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(dog));
        when(breedRepository.existsBySpeciesIdAndNameIgnoreCase(1L, "Poodle")).thenReturn(true);
        assertThatThrownBy(() -> service.createBreed(1L, request)).isInstanceOf(BadRequestException.class);

        when(breedRepository.existsBySpeciesIdAndNameIgnoreCase(1L, "Poodle")).thenReturn(false);
        when(breedRepository.save(any(Breed.class))).thenAnswer(invocation -> { Breed value = invocation.getArgument(0); value.setId(2L); return value; });
        assertThat(service.createBreed(1L, request).getName()).isEqualTo("Poodle");
    }
}
