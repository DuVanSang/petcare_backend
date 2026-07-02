package com.petcare.backend.repository;

import com.petcare.backend.model.CareReminder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareReminderRepository extends JpaRepository<CareReminder, Long> {
    long countByActiveTrue();

    List<CareReminder> findByCreatedByIdAndActiveTrueOrderByNextDueAtAsc(Long userId);

    Optional<CareReminder> findByIdAndCreatedById(Long id, Long userId);

    List<CareReminder> findByVaccinationIdAndActiveTrue(Long vaccinationId);
}
