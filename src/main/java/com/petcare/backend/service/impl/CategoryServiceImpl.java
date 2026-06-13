package com.petcare.backend.service.impl;

import com.petcare.backend.dto.pet.response.BreedResponse;
import com.petcare.backend.dto.pet.response.SpeciesResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.repository.BreedRepository;
import com.petcare.backend.repository.SpeciesRepository;
import com.petcare.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
