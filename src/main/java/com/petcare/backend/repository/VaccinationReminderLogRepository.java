package com.petcare.backend.repository;

import com.petcare.backend.model.VaccinationReminderLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaccinationReminderLogRepository extends JpaRepository<VaccinationReminderLog, Long>,
        JpaSpecificationExecutor<VaccinationReminderLog> {
    Optional<VaccinationReminderLog> findByVaccinationIdAndUserIdAndStage(
            Long vaccinationId,
            Long userId,
            VaccinationReminderLog.VaccinationReminderStage stage
    );

    List<VaccinationReminderLog> findByVaccinationIdAndStatus(
            Long vaccinationId,
            VaccinationReminderLog.VaccinationReminderStatus status
    );
}
