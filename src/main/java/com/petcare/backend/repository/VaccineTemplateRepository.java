package com.petcare.backend.repository;

import com.petcare.backend.model.VaccineTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VaccineTemplateRepository extends JpaRepository<VaccineTemplate, Long> {
    List<VaccineTemplate> findBySpeciesId(Long speciesId);
}
