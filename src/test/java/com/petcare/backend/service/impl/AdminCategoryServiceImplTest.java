package com.petcare.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.admin.category.request.AdminCreateBreedRequest;
import com.petcare.backend.dto.admin.category.request.AdminCreateSpeciesRequest;
import com.petcare.backend.dto.admin.category.request.AdminUpdateBreedRequest;
import com.petcare.backend.dto.admin.category.request.AdminUpdateSpeciesRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Breed;
import com.petcare.backend.model.Species;
import com.petcare.backend.repository.BreedRepository;
import com.petcare.backend.repository.SpeciesRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"rawtypes", "unchecked"})
class AdminCategoryServiceImplTest {
    @Mock private SpeciesRepository speciesRepository;
    @Mock private BreedRepository breedRepository;
    private AdminCategoryServiceImpl service;

    @BeforeEach void setUp() { service = new AdminCategoryServiceImpl(speciesRepository, breedRepository); }

    @Test
    void getSpeciesMapsDataFiltersAndPageBoundary() {
        when(speciesRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(species(1L, "Dog", true))));
        var response = service.getSpecies(" Dog ", true, 0, 101);
        assertEquals(1, response.getContent().size()); assertEquals("Dog", response.getContent().getFirst().getName());
        ArgumentCaptor<Specification> spec = ArgumentCaptor.forClass(Specification.class); ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(speciesRepository).findAll(spec.capture(), page.capture()); assertEquals(100, page.getValue().getPageSize()); assertEquals("name: ASC", page.getValue().getSort().toString()); execute(spec.getValue());
    }

    @Test
    void getSpeciesSupportsEmptyPageNoFiltersAndRejectsInvalidPaging() {
        when(speciesRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, service.getSpecies(" ", null, 0, 1).getContent().size());
        ArgumentCaptor<Specification> spec = ArgumentCaptor.forClass(Specification.class); verify(speciesRepository).findAll(spec.capture(), any(Pageable.class)); execute(spec.getValue());
        assertThrows(BadRequestException.class, () -> service.getSpecies(null, null, -1, 1));
        assertThrows(BadRequestException.class, () -> service.getSpecies(null, null, 0, 0));
    }

    @Test
    void speciesDetailCreateAndUpdateMapDataAndOptionalFields() {
        Species existing = species(2L, "Cat", true); existing.setIconUrl("old");
        when(speciesRepository.findById(2L)).thenReturn(Optional.of(existing)); when(speciesRepository.save(any(Species.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var detail = service.getSpeciesDetail(2L); assertEquals(2L, detail.getId()); assertEquals("Cat", detail.getName());
        AdminCreateSpeciesRequest create = new AdminCreateSpeciesRequest(); create.setName("  Bird "); create.setIconUrl(" ");
        when(speciesRepository.save(any(Species.class))).thenAnswer(invocation -> { Species saved = invocation.getArgument(0); if (saved.getId() == null) saved.setId(3L); return saved; });
        var created = service.createSpecies(create); assertEquals(3L, created.getId()); assertEquals("Bird", created.getName()); assertNull(created.getIconUrl());
        AdminUpdateSpeciesRequest update = new AdminUpdateSpeciesRequest(); update.setName(" Kitten "); update.setIconUrl(" "); update.setActive(false);
        var updated = service.updateSpecies(2L, update); assertEquals("Kitten", updated.getName()); assertNull(updated.getIconUrl()); assertEquals(false, updated.getActive());
        verify(breedRepository).save(any(Breed.class));
    }

    @Test
    void speciesCreateAndUpdateRejectBlankDuplicateAndMissingSpecies() {
        AdminCreateSpeciesRequest create = new AdminCreateSpeciesRequest(); create.setName(" ");
        assertThrows(BadRequestException.class, () -> service.createSpecies(create)); create.setName("Dog"); when(speciesRepository.existsByNameIgnoreCase("Dog")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.createSpecies(create));
        when(speciesRepository.findById(9L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.getSpeciesDetail(9L));
        Species species = species(1L, "Dog", true); when(speciesRepository.findById(1L)).thenReturn(Optional.of(species)); AdminUpdateSpeciesRequest update = new AdminUpdateSpeciesRequest(); update.setName("Cat"); when(speciesRepository.existsByNameIgnoreCaseAndIdNot("Cat", 1L)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.updateSpecies(1L, update)); update.setName(" "); assertThrows(BadRequestException.class, () -> service.updateSpecies(1L, update));
    }

    @Test
    void getBreedsMapsFiltersEmptyAndRejectsUnknownSpecies() {
        Species species = species(1L, "Dog", true); when(speciesRepository.existsById(1L)).thenReturn(true);
        when(breedRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(breed(4L, species, "Husky", true))));
        var response = service.getBreeds(1L, " Hus ", true, 0, 20); assertEquals("Husky", response.getContent().getFirst().getName());
        ArgumentCaptor<Specification> spec = ArgumentCaptor.forClass(Specification.class); verify(breedRepository).findAll(spec.capture(), any(Pageable.class)); execute(spec.getValue());
        when(breedRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of())); assertEquals(0, service.getBreeds(null, null, null, 0, 1).getContent().size());
        when(speciesRepository.existsById(9L)).thenReturn(false); assertThrows(BadRequestException.class, () -> service.getBreeds(9L, null, null, 0, 1));
    }

    @Test
    void breedDetailCreateAndUpdateMapAndMoveBreedToNewSpecies() {
        Species dog = species(1L, "Dog", true), cat = species(2L, "Cat", true); Breed husky = breed(5L, dog, "Husky", true);
        when(breedRepository.findById(5L)).thenReturn(Optional.of(husky)); when(speciesRepository.findById(1L)).thenReturn(Optional.of(dog)); when(speciesRepository.findById(2L)).thenReturn(Optional.of(cat)); when(breedRepository.save(any(Breed.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("Dog", service.getBreedDetail(5L).getSpeciesName());
        AdminCreateBreedRequest create = new AdminCreateBreedRequest(); create.setSpeciesId(1L); create.setName(" Poodle "); when(breedRepository.save(any(Breed.class))).thenAnswer(invocation -> { Breed saved = invocation.getArgument(0); if (saved.getId() == null) saved.setId(6L); return saved; });
        assertEquals("Poodle", service.createBreed(create).getName());
        AdminUpdateBreedRequest update = new AdminUpdateBreedRequest(); update.setSpeciesId(2L); update.setName(" Persian "); update.setActive(false);
        var changed = service.updateBreed(5L, update); assertEquals(2L, changed.getSpeciesId()); assertEquals("Persian", changed.getName()); assertEquals(false, changed.getActive());
    }

    @Test
    void breedCreateAndUpdateRejectDuplicateBlankAndNotFound() {
        when(speciesRepository.findById(1L)).thenReturn(Optional.empty()); AdminCreateBreedRequest create = new AdminCreateBreedRequest(); create.setSpeciesId(1L); create.setName("Husky"); assertThrows(ResourceNotFoundException.class, () -> service.createBreed(create));
        Species species = species(1L, "Dog", true); when(speciesRepository.findById(1L)).thenReturn(Optional.of(species)); when(breedRepository.existsBySpeciesIdAndNameIgnoreCase(1L, "Husky")).thenReturn(true); assertThrows(BadRequestException.class, () -> service.createBreed(create)); create.setName(" "); assertThrows(BadRequestException.class, () -> service.createBreed(create));
        when(breedRepository.findById(9L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.getBreedDetail(9L));
        Breed breed = breed(5L, species, "Husky", true); when(breedRepository.findById(5L)).thenReturn(Optional.of(breed)); AdminUpdateBreedRequest update = new AdminUpdateBreedRequest(); update.setName("Poodle"); when(breedRepository.existsBySpeciesIdAndNameIgnoreCaseAndIdNot(1L, "Poodle", 5L)).thenReturn(true); assertThrows(BadRequestException.class, () -> service.updateBreed(5L, update)); update.setName(" "); assertThrows(BadRequestException.class, () -> service.updateBreed(5L, update));
    }

    @Test
    void updateOptionalFieldsAndBreedMoveWithoutNameCoverRemainingUpdatePartitions() {
        Species dog = species(1L, "Dog", true), cat = species(2L, "Cat", true); Breed breed = breed(5L, dog, "Husky", true);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(dog)); when(speciesRepository.findById(2L)).thenReturn(Optional.of(cat)); when(breedRepository.findById(5L)).thenReturn(Optional.of(breed)); when(speciesRepository.save(any(Species.class))).thenAnswer(i -> i.getArgument(0)); when(breedRepository.save(any(Breed.class))).thenAnswer(i -> i.getArgument(0));
        AdminUpdateSpeciesRequest speciesUpdate = new AdminUpdateSpeciesRequest(); speciesUpdate.setIconUrl(" icon.png ");
        assertEquals("icon.png", service.updateSpecies(1L, speciesUpdate).getIconUrl());
        AdminUpdateBreedRequest unchanged = new AdminUpdateBreedRequest();
        assertEquals("Husky", service.updateBreed(5L, unchanged).getName());
        AdminUpdateBreedRequest move = new AdminUpdateBreedRequest(); move.setSpeciesId(2L); when(breedRepository.existsBySpeciesIdAndNameIgnoreCaseAndIdNot(2L, "Husky", 5L)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.updateBreed(5L, move));
        move.setSpeciesId(9L); when(speciesRepository.findById(9L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.updateBreed(5L, move));
    }

    @Test
    void getBreedsWithoutFiltersExecutesAllFalseSpecificationBranches() {
        when(breedRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, service.getBreeds(null, " ", null, 0, 1).getContent().size());
        ArgumentCaptor<Specification> spec = ArgumentCaptor.forClass(Specification.class); verify(breedRepository).findAll(spec.capture(), any(Pageable.class)); execute(spec.getValue());
    }

    private void execute(Specification specification) { specification.toPredicate(root(), mock(CriteriaQuery.class), criteriaBuilder()); }
    private Root root() { Root root = mock(Root.class); Path path = mock(Path.class); when(root.get(any(String.class))).thenReturn(path); when(path.get(any(String.class))).thenReturn(path); return root; }
    private CriteriaBuilder criteriaBuilder() { CriteriaBuilder cb = mock(CriteriaBuilder.class); Predicate predicate = mock(Predicate.class); when(cb.lower(any())).thenReturn(mock(jakarta.persistence.criteria.Expression.class)); when(cb.like(any(), any(String.class))).thenReturn(predicate); when(cb.equal(any(), any())).thenReturn(predicate); when(cb.and(any(Predicate[].class))).thenReturn(predicate); return cb; }
    private Species species(Long id, String name, Boolean active) { Species species = new Species(); species.setId(id); species.setName(name); species.setActive(active); return species; }
    private Breed breed(Long id, Species species, String name, Boolean active) { Breed breed = new Breed(); breed.setId(id); breed.setSpecies(species); breed.setName(name); breed.setActive(active); return breed; }
}
