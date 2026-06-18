package com.petcare.backend.repository;

import com.petcare.backend.model.PetVaccination;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetVaccinationRepository extends JpaRepository<PetVaccination, Long> {
    List<PetVaccination> findByPetIdOrderByScheduledDateAsc(Long petId);

    List<PetVaccination> findByPetIdAndStatusOrderByScheduledDateAsc(
            Long petId,
            PetVaccination.VaccinationStatus status
    );

    Optional<PetVaccination> findByIdAndPetId(Long id, Long petId);
}
