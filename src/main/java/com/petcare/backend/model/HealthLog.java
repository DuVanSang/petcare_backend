package com.petcare.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "health_logs",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pet_logged_date",
                columnNames = {"pet_id", "logged_date"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class HealthLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "logged_date", nullable = false)
    private LocalDate loggedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Appetite appetite = Appetite.normal;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false, length = 20)
    private ActivityLevel activityLevel = ActivityLevel.moderate;

    @Column(name = "abnormal_event", columnDefinition = "TEXT")
    private String abnormalEvent;

    @Column(name = "treatment_notes", columnDefinition = "TEXT")
    private String treatmentNotes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logged_by", nullable = false)
    private User loggedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Appetite {
        good, normal, poor
    }

    public enum ActivityLevel {
        very_active, active, moderate, low
    }
}
