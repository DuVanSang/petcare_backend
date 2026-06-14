package com.petcare.backend.model;

import com.petcare.backend.model.converter.CommentStatusConverter;
import com.petcare.backend.model.enums.CommentStatus;
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
        name = "post_comments",
        indexes = {
                @Index(name = "idx_comments_post_created", columnList = "post_id,created_at"),
                @Index(name = "idx_comments_parent", columnList = "parent_comment_id"),
                @Index(name = "idx_comments_root", columnList = "root_comment_id"),
                @Index(name = "idx_comments_user", columnList = "user_id"),
                @Index(name = "idx_comments_status", columnList = "status")
        }
)
public class PostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private User user;

    @Column(name = "parent_comment_id", columnDefinition = "BIGINT UNSIGNED")
    private Long parentCommentId;

    @Column(name = "root_comment_id", columnDefinition = "BIGINT UNSIGNED")
    private Long rootCommentId;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Integer depth = 0;

    // TODO: Add comment media support in a later phase.
    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Builder.Default
    @Convert(converter = CommentStatusConverter.class)
    @Column(nullable = false, length = 20)
    private CommentStatus status = CommentStatus.VISIBLE;

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
