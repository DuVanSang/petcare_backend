package com.petcare.backend.repository;

import com.petcare.backend.model.Species;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpeciesRepository extends JpaRepository<Species, Long> {
    boolean existsByNameIgnoreCase(String name);
}
