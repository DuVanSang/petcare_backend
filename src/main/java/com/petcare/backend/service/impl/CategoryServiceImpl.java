package com.petcare.backend.service.impl;

import com.petcare.backend.dto.pet.request.CreateBreedRequest;
import com.petcare.backend.dto.pet.request.CreateSpeciesRequest;
import com.petcare.backend.dto.pet.response.BreedResponse;
import com.petcare.backend.dto.pet.response.SpeciesResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Breed;
import com.petcare.backend.model.Species;
import com.petcare.backend.repository.BreedRepository;
import com.petcare.backend.repository.SpeciesRepository;
import com.petcare.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;

    @Override
    public List<SpeciesResponse> getAllSpecies() {
        return speciesRepository.findAll().stream()
                .map(SpeciesResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<BreedResponse> getBreedsBySpecies(Long speciesId) {
        if (!speciesRepository.existsById(speciesId)) {
            throw new BadRequestException("Loài không tồn tại");
        }
        return breedRepository.findBySpeciesId(speciesId).stream()
                .map(BreedResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SpeciesResponse createSpecies(CreateSpeciesRequest request) {
        String name = request.getName().trim();
        if (speciesRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Loài \"" + name + "\" đã tồn tại");
        }

        Species species = new Species();
        species.setName(name);
        if (StringUtils.hasText(request.getIconUrl())) {
            species.setIconUrl(request.getIconUrl().trim());
        }
        species = speciesRepository.save(species);

        Breed otherBreed = new Breed();
        otherBreed.setSpecies(species);
        otherBreed.setName("Khác");
        breedRepository.save(otherBreed);

        return SpeciesResponse.from(species);
    }

    @Override
    @Transactional
    public BreedResponse createBreed(Long speciesId, CreateBreedRequest request) {
        Species species = speciesRepository.findById(speciesId)
                .orElseThrow(() -> new BadRequestException("Loài không tồn tại"));

        String name = request.getName().trim();
        if (breedRepository.existsBySpeciesIdAndNameIgnoreCase(speciesId, name)) {
            throw new BadRequestException("Giống \"" + name + "\" đã tồn tại trong loài này");
        }

        Breed breed = new Breed();
        breed.setSpecies(species);
        breed.setName(name);
        return BreedResponse.from(breedRepository.save(breed));
    }
}
