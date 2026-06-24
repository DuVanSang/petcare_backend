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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vaccine_templates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vaccine_template_rule",
                columnNames = {"species_id", "series_code", "target_stage", "dose_number"}
        ),
        indexes = @Index(
                name = "idx_vaccine_template_stage_active",
                columnList = "species_id,target_stage,is_active"
        ))
@Getter
@Setter
@NoArgsConstructor
public class VaccineTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @Column(name = "vaccine_name", nullable = false, length = 150)
    private String vaccineName;

    @Column(name = "series_code", length = 50)
    private String seriesCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_stage", length = 20)
    private TargetStage targetStage;

    @Column(name = "dose_number", nullable = false)
    private Integer doseNumber = 1;

    // Giữ cột legacy để tương thích dữ liệu hiện tại trong giai đoạn chuyển đổi.
    @Column(name = "recommended_age_weeks", nullable = false)
    private Integer recommendedAgeWeeks;

    @Column(name = "minimum_age_weeks")
    private Integer minimumAgeWeeks;

    @Column(name = "interval_from_previous_days")
    private Integer intervalFromPreviousDays;

    @Column(name = "booster_interval_months")
    private Integer boosterIntervalMonths;

    @Column(name = "is_optional", nullable = false)
    private Boolean optional = false;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    public int effectiveMinimumAgeWeeks() {
        return minimumAgeWeeks != null ? minimumAgeWeeks : recommendedAgeWeeks;
    }

    public enum TargetStage {
        PUPPY, CATCH_UP, ADULT
    }
}
