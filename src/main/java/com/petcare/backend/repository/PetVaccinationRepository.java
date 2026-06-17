package com.petcare.backend.repository;

import com.petcare.backend.model.PetVaccination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetVaccinationRepository extends JpaRepository<PetVaccination, Long> {
}
