package com.petcare.backend.repository;

import com.petcare.backend.model.Breed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface BreedRepository extends JpaRepository<Breed, Long> {
    List<Breed> findBySpeciesId(Long speciesId);
    Page<Breed> findBySpeciesId(Long speciesId, Pageable pageable);

    boolean existsBySpeciesIdAndNameIgnoreCase(Long speciesId, String name);
}
