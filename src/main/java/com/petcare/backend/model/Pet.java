package com.petcare.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pets")
@Getter
@Setter
@NoArgsConstructor
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id")
    private Species species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id")
    private Breed breed;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "estimated_age_months")
    private Integer estimatedAgeMonths;

    @Column(name = "current_weight", precision = 5, scale = 2)
    private BigDecimal currentWeight;

    @Column(name = "color_features", columnDefinition = "TEXT")
    private String colorFeatures;

    @Enumerated(EnumType.STRING)
    @Column(name = "spayed_status", length = 20)
    private SpayedStatus spayedStatus = SpayedStatus.unknown;

    @Column(name = "microchip_number", length = 50)
    private String microchipNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PetStatus status = PetStatus.active;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PetCoParent> coParents = new ArrayList<>();

    // ========== Enums ==========

    public enum Gender {
        male, female, unknown
    }

    public enum SpayedStatus {
        yes, no, unknown
    }

    public enum PetStatus {
        active, deceased, lost, rehomed
    }
}
