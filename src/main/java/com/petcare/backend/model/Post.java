package com.petcare.backend.model;

import com.petcare.backend.model.converter.PostPrivacyConverter;
import com.petcare.backend.model.converter.PostStatusConverter;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
import java.time.LocalDateTime;
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
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_user_status_created", columnList = "user_id,status,created_at"),
                @Index(name = "idx_posts_pet_status_created", columnList = "pet_id,status,created_at"),
                @Index(name = "idx_posts_privacy_status_created", columnList = "privacy,status,created_at")
        }
)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User user;

    // TODO: Add Pet relationship and permission checks after the Pet module exists.
    @Column(name = "pet_id", columnDefinition = "BIGINT UNSIGNED")
    private Long petId;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Builder.Default
    @Convert(converter = PostPrivacyConverter.class)
    @Column(nullable = false, length = 20)
    private PostPrivacy privacy = PostPrivacy.PUBLIC;

    @Builder.Default
    @Convert(converter = PostStatusConverter.class)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.PUBLISHED;

    @Builder.Default
    @Column(name = "comments_locked", nullable = false)
    private Boolean commentsLocked = false;

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
}
