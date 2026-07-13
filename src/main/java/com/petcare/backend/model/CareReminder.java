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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "care_reminders", indexes = {
        @Index(name = "idx_care_reminders_creator_due", columnList = "created_by,is_active,next_due_at"),
        @Index(name = "idx_care_reminders_pet_active", columnList = "pet_id,is_active"),
        @Index(name = "idx_care_reminders_vaccination_active", columnList = "vaccination_id,is_active")
})
public class CareReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccination_id", columnDefinition = "BIGINT UNSIGNED")
    private PetVaccination vaccination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReminderCategory category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderFrequency frequency;

    @Column(name = "next_due_at")
    private Instant nextDueAt;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "interval_value", nullable = false)
    private Integer intervalValue = 1;

    @Column(name = "before_duration_minutes", nullable = false)
    private Integer beforeDurationMinutes = 0;

    @Column(name = "vaccination_offset_minutes")
    private Long vaccinationOffsetMinutes;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

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

    public enum ReminderCategory {
        vaccination,
        bathing,
        nail_clipping,
        deworming,
        medication,
        medical_checkup,
        other
    }

    public enum ReminderFrequency {
        once,
        daily,
        weekly,
        monthly,
        yearly,
        quarterly
    }
}
