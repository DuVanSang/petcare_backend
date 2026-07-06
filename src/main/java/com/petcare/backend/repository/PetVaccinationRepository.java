package com.petcare.backend.repository;

import com.petcare.backend.model.PetVaccination;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetVaccinationRepository extends JpaRepository<PetVaccination, Long> {
    boolean existsByPetId(Long petId);

    long countByStatus(PetVaccination.VaccinationStatus status);

    long countByPetId(Long petId);

    List<PetVaccination> findByPetIdOrderByScheduledDateAsc(Long petId);

    List<PetVaccination> findByPetIdAndStatusOrderByScheduledDateAsc(
            Long petId,
            PetVaccination.VaccinationStatus status
    );

    long countByPetIdAndStatus(Long petId, PetVaccination.VaccinationStatus status);

    Optional<PetVaccination> findByIdAndPetId(Long id, Long petId);

    List<PetVaccination> findByPetIdAndStatusOrderBySeriesCodeAscDoseNumberAsc(
            Long petId,
            PetVaccination.VaccinationStatus status
    );

    List<PetVaccination> findByPetIdAndSeriesCodeAndDoseNumberGreaterThanOrderByDoseNumberAsc(
            Long petId,
            String seriesCode,
            Integer doseNumber
    );

    boolean existsByPetIdAndSeriesCodeAndScheduledDateAndStatusNot(
            Long petId,
            String seriesCode,
            LocalDate scheduledDate,
            PetVaccination.VaccinationStatus excludedStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PetVaccination> findByStatusInAndScheduledDateBetween(
            Collection<PetVaccination.VaccinationStatus> statuses,
            LocalDate from,
            LocalDate to
    );

    List<PetVaccination> findByStatusAndScheduledDateBefore(
            PetVaccination.VaccinationStatus status,
            LocalDate date
    );
}
