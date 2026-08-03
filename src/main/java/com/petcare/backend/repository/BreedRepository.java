package com.petcare.backend.repository;

import com.petcare.backend.model.Breed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface BreedRepository extends JpaRepository<Breed, Long>, JpaSpecificationExecutor<Breed> {
    List<Breed> findBySpeciesId(Long speciesId);

    List<Breed> findBySpeciesIdAndActiveTrue(Long speciesId);

    Page<Breed> findBySpeciesId(Long speciesId, Pageable pageable);

    Page<Breed> findBySpeciesIdAndActiveTrue(Long speciesId, Pageable pageable);

    Optional<Breed> findBySpeciesIdAndNameIgnoreCase(Long speciesId, String name);

    boolean existsBySpeciesIdAndNameIgnoreCase(Long speciesId, String name);

    boolean existsBySpeciesIdAndNameIgnoreCaseAndIdNot(Long speciesId, String name, Long id);
}
