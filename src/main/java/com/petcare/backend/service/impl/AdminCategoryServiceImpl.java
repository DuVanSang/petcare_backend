package com.petcare.backend.service.impl;

import com.petcare.backend.dto.admin.category.request.AdminCreateBreedRequest;
import com.petcare.backend.dto.admin.category.request.AdminCreateSpeciesRequest;
import com.petcare.backend.dto.admin.category.request.AdminUpdateBreedRequest;
import com.petcare.backend.dto.admin.category.request.AdminUpdateSpeciesRequest;
import com.petcare.backend.dto.admin.category.response.AdminBreedResponse;
import com.petcare.backend.dto.admin.category.response.AdminSpeciesResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Breed;
import com.petcare.backend.model.Species;
import com.petcare.backend.repository.BreedRepository;
import com.petcare.backend.repository.SpeciesRepository;
import com.petcare.backend.service.AdminCategoryService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {
    private static final int MAX_PAGE_SIZE = 100;

    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminSpeciesResponse> getSpecies(String keyword, Boolean active, int page, int size) {
        return PageResponse.from(speciesRepository
                .findAll(speciesSpecification(keyword, active), pageable(page, size))
                .map(AdminSpeciesResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSpeciesResponse getSpeciesDetail(Long speciesId) {
        return AdminSpeciesResponse.from(getSpeciesOrThrow(speciesId));
    }

    @Override
    @Transactional
    public AdminSpeciesResponse createSpecies(AdminCreateSpeciesRequest request) {
        String name = normalizeRequiredName(request.getName(), "Tên loài không được để trống");
        if (speciesRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Loài \"" + name + "\" đã tồn tại");
        }

        Species species = new Species();
        species.setName(name);
        species.setIconUrl(trimToNull(request.getIconUrl()));
        species.setActive(true);
        Species savedSpecies = speciesRepository.save(species);

        Breed otherBreed = new Breed();
        otherBreed.setSpecies(savedSpecies);
        otherBreed.setName("Khác");
        otherBreed.setActive(true);
        breedRepository.save(otherBreed);

        return AdminSpeciesResponse.from(savedSpecies);
    }

    @Override
    @Transactional
    public AdminSpeciesResponse updateSpecies(Long speciesId, AdminUpdateSpeciesRequest request) {
        Species species = getSpeciesOrThrow(speciesId);

        if (request.getName() != null) {
            String name = normalizeRequiredName(request.getName(), "Tên loài không được để trống");
            if (speciesRepository.existsByNameIgnoreCaseAndIdNot(name, speciesId)) {
                throw new BadRequestException("Loài \"" + name + "\" đã tồn tại");
            }
            species.setName(name);
        }

        if (request.getIconUrl() != null) {
            species.setIconUrl(trimToNull(request.getIconUrl()));
        }

        if (request.getActive() != null) {
            species.setActive(request.getActive());
        }

        return AdminSpeciesResponse.from(speciesRepository.save(species));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminBreedResponse> getBreeds(
            Long speciesId,
            String keyword,
            Boolean active,
            int page,
            int size
    ) {
        if (speciesId != null && !speciesRepository.existsById(speciesId)) {
            throw new BadRequestException("Loài không tồn tại");
        }

        return PageResponse.from(breedRepository
                .findAll(breedSpecification(speciesId, keyword, active), pageable(page, size))
                .map(AdminBreedResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBreedResponse getBreedDetail(Long breedId) {
        return AdminBreedResponse.from(getBreedOrThrow(breedId));
    }

    @Override
    @Transactional
    public AdminBreedResponse createBreed(AdminCreateBreedRequest request) {
        Species species = getSpeciesOrThrow(request.getSpeciesId());
        String name = normalizeRequiredName(request.getName(), "Tên giống không được để trống");

        if (breedRepository.existsBySpeciesIdAndNameIgnoreCase(species.getId(), name)) {
            throw new BadRequestException("Giống \"" + name + "\" đã tồn tại trong loài này");
        }

        Breed breed = new Breed();
        breed.setSpecies(species);
        breed.setName(name);
        breed.setActive(true);
        return AdminBreedResponse.from(breedRepository.save(breed));
    }

    @Override
    @Transactional
    public AdminBreedResponse updateBreed(Long breedId, AdminUpdateBreedRequest request) {
        Breed breed = getBreedOrThrow(breedId);
        Species targetSpecies = breed.getSpecies();

        if (request.getSpeciesId() != null && !request.getSpeciesId().equals(targetSpecies.getId())) {
            targetSpecies = getSpeciesOrThrow(request.getSpeciesId());
        }

        if (request.getName() != null) {
            String name = normalizeRequiredName(request.getName(), "Tên giống không được để trống");
            if (breedRepository.existsBySpeciesIdAndNameIgnoreCaseAndIdNot(targetSpecies.getId(), name, breedId)) {
                throw new BadRequestException("Giống \"" + name + "\" đã tồn tại trong loài này");
            }
            breed.setName(name);
        } else if (!targetSpecies.getId().equals(breed.getSpecies().getId())
                && breedRepository.existsBySpeciesIdAndNameIgnoreCaseAndIdNot(
                        targetSpecies.getId(),
                        breed.getName(),
                        breedId
                )) {
            throw new BadRequestException("Giống \"" + breed.getName() + "\" đã tồn tại trong loài này");
        }

        breed.setSpecies(targetSpecies);

        if (request.getActive() != null) {
            breed.setActive(request.getActive());
        }

        return AdminBreedResponse.from(breedRepository.save(breed));
    }

    private Specification<Species> speciesSpecification(String keyword, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<Breed> breedSpecification(Long speciesId, String keyword, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (speciesId != null) {
                predicates.add(cb.equal(root.get("species").get("id"), speciesId));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable pageable(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Số trang không được âm");
        }
        if (size <= 0) {
            throw new BadRequestException("Kích thước trang phải lớn hơn 0");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.ASC, "name"));
    }

    private Species getSpeciesOrThrow(Long speciesId) {
        return speciesRepository.findById(speciesId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loài"));
    }

    private Breed getBreedOrThrow(Long breedId) {
        return breedRepository.findById(breedId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giống"));
    }

    private String normalizeRequiredName(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
