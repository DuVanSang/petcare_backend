package com.petcare.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "pet_moments",
        indexes = {
                @Index(name = "idx_pet_moments_pet_created", columnList = "pet_id,created_at"),
                @Index(name = "idx_pet_moments_user_created", columnList = "user_id,created_at"),
                @Index(name = "idx_pet_moments_created", columnList = "created_at")
        }
)
public class PetMoment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User user;

    @Column(name = "media_url", nullable = false, length = 1000)
    private String mediaUrl;

    @Column(length = 255)
    private String caption;

    @Column(name = "location_name", length = 255)
    private String locationName;

    @Column(name = "mood_tag", length = 50)
    private String moodTag;

    @Column(name = "audience", length = 30)
    private String audience;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
