package com.petcare.backend.repository;

import com.petcare.backend.model.Breed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BreedRepository extends JpaRepository<Breed, Long> {
    List<Breed> findBySpeciesId(Long speciesId);

    boolean existsBySpeciesIdAndNameIgnoreCase(Long speciesId, String name);
}
