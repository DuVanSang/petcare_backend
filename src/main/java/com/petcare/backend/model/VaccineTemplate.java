package com.petcare.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vaccine_templates")
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

    @Column(name = "dose_number", nullable = false)
    private Integer doseNumber = 1;

    @Column(name = "recommended_age_weeks", nullable = false)
    private Integer recommendedAgeWeeks;

    @Column(columnDefinition = "TEXT")
    private String description;
}
