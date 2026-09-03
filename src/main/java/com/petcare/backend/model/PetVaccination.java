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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "pet_vaccinations", indexes = {
        @Index(name = "idx_pet_vaccination_schedule", columnList = "pet_id,status,scheduled_date"),
        @Index(name = "idx_pet_vaccination_series", columnList = "pet_id,series_code,dose_number")
})
@Getter
@Setter
@NoArgsConstructor
public class PetVaccination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_template_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private VaccineTemplate vaccineTemplate;

    @Column(name = "vaccine_name", nullable = false, length = 150)
    private String vaccineName;

    @Column(name = "series_code", length = 50)
    private String seriesCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_stage", length = 20)
    private VaccineTemplate.TargetStage targetStage;

    @Column(name = "dose_number", nullable = false)
    private Integer doseNumber = 1;

    @Column(name = "minimum_age_weeks")
    private Integer minimumAgeWeeks;

    @Column(name = "interval_from_previous_days")
    private Integer intervalFromPreviousDays;

    @Column(name = "booster_interval_months")
    private Integer boosterIntervalMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VaccinationStatus status = VaccinationStatus.scheduled;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_source", length = 20)
    private ScheduleSource scheduleSource;

    @Column(name = "schedule_locked", nullable = false)
    private Boolean scheduleLocked = false;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Column(name = "administered_by", length = 150)
    private String administeredBy;

    @Column(name = "clinic_name", length = 150)
    private String clinicName;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "medical_proof_url", length = 500)
    private String medicalProofUrl;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum VaccinationStatus {
        proposed, scheduled, completed, skipped, overdue, cancelled
    }

    public enum ScheduleSource {
        AUTO_TEMPLATE, MANUAL
    }
}
