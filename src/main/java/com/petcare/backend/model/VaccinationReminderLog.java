package com.petcare.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vaccination_reminder_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vaccination_reminder_recipient_stage",
                columnNames = {"vaccination_id", "user_id", "stage"}
        ),
        indexes = @Index(name = "idx_vaccination_reminder_status_time", columnList = "status,scheduled_at"))
public class VaccinationReminderLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vaccination_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private PetVaccination vaccination;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VaccinationReminderStage stage;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VaccinationReminderStatus status = VaccinationReminderStatus.pending;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum VaccinationReminderStage {
        BEFORE_7_DAYS(-7),
        BEFORE_1_DAY(-1),
        DUE_TODAY(0),
        OVERDUE_1_DAY(1),
        OVERDUE_3_DAYS(3),
        OVERDUE_7_DAYS(7),
        OVERDUE_14_DAYS(14);

        private final int dayOffset;

        VaccinationReminderStage(int dayOffset) {
            this.dayOffset = dayOffset;
        }

        public int getDayOffset() {
            return dayOffset;
        }
    }

    public enum VaccinationReminderStatus {
        pending,
        notified,
        cancelled
    }
}
