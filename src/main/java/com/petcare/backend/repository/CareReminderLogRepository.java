package com.petcare.backend.repository;

import com.petcare.backend.model.CareReminderLog;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CareReminderLogRepository extends JpaRepository<CareReminderLog, Long>,
        JpaSpecificationExecutor<CareReminderLog> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT log FROM CareReminderLog log
            JOIN FETCH log.reminder reminder
            JOIN FETCH reminder.createdBy
            JOIN FETCH reminder.pet
            WHERE log.status = :status
              AND log.dueAt <= :now
              AND reminder.active = true
            ORDER BY log.dueAt ASC
            """)
    List<CareReminderLog> findDueForUpdate(
            @Param("status") CareReminderLog.ReminderLogStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    List<CareReminderLog> findByReminderIdOrderByDueAtDesc(Long reminderId);

    Optional<CareReminderLog> findFirstByReminderIdAndStatusInOrderByDueAtDesc(
            Long reminderId,
            Collection<CareReminderLog.ReminderLogStatus> statuses
    );

    Optional<CareReminderLog> findFirstByReminderIdAndStatusInAndDueAtLessThanEqualOrderByDueAtDesc(
            Long reminderId,
            Collection<CareReminderLog.ReminderLogStatus> statuses,
            Instant now
    );

    Optional<CareReminderLog> findFirstByReminderIdAndStatusInOrderByDueAtAsc(
            Long reminderId,
            Collection<CareReminderLog.ReminderLogStatus> statuses
    );

    List<CareReminderLog> findByReminderIdAndStatusIn(
            Long reminderId,
            Collection<CareReminderLog.ReminderLogStatus> statuses
    );

    boolean existsByReminderIdAndStatusIn(Long reminderId, Collection<CareReminderLog.ReminderLogStatus> statuses);

    boolean existsByReminderIdAndStatusInAndDueAtLessThanEqual(
            Long reminderId,
            Collection<CareReminderLog.ReminderLogStatus> statuses,
            Instant dueAt
    );

    boolean existsByReminderIdAndStatusInAndDueAtAfter(
            Long reminderId,
            Collection<CareReminderLog.ReminderLogStatus> statuses,
            Instant dueAt
    );

    boolean existsByReminderIdAndStatus(Long reminderId, CareReminderLog.ReminderLogStatus status);

    boolean existsByReminderIdAndDueAt(Long reminderId, Instant dueAt);

    Optional<CareReminderLog> findByReminderIdAndDueAt(Long reminderId, Instant dueAt);

    boolean existsByReminderIdAndDueAtAndIdNot(Long reminderId, Instant dueAt, Long id);
}
