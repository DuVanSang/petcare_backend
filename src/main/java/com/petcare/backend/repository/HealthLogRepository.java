package com.petcare.backend.repository;

import com.petcare.backend.model.HealthLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthLogRepository extends JpaRepository<HealthLog, Long> {

    Optional<HealthLog> findByPetIdAndLoggedDate(Long petId, LocalDate loggedDate);

    List<HealthLog> findByPetIdOrderByLoggedDateDesc(Long petId);

    Optional<HealthLog> findFirstByPetIdOrderByLoggedDateDesc(Long petId);
}
