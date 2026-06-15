package com.petcare.backend.repository;

import com.petcare.backend.model.Post;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByUser_IdAndStatusNotOrderByCreatedAtDesc(
            Long userId,
            PostStatus status,
            Pageable pageable
    );

    Page<Post> findByUser_IdAndStatusOrderByCreatedAtDesc(
            Long userId,
            PostStatus status,
            Pageable pageable
    );

    Page<Post> findByUser_IdAndStatusAndPrivacyOrderByCreatedAtDesc(
            Long userId,
            PostStatus status,
            PostPrivacy privacy,
            Pageable pageable
    );

    Page<Post> findByStatusAndPrivacyOrderByCreatedAtDesc(
            PostStatus status,
            PostPrivacy privacy,
            Pageable pageable
    );

    Page<Post> findByPetIdAndStatusOrderByCreatedAtDesc(
            Long petId,
            PostStatus status,
            Pageable pageable
    );

    Page<Post> findByPetIdAndStatusAndPrivacyOrderByCreatedAtDesc(
            Long petId,
            PostStatus status,
            PostPrivacy privacy,
            Pageable pageable
    );

    Optional<Post> findByIdAndStatusNot(Long id, PostStatus status);

    @Query("""
            SELECT p FROM Post p
            WHERE p.petId = :petId
              AND p.status <> :deletedStatus
              AND (
                    p.user.id = :currentUserId
                    OR (p.status = :publishedStatus AND p.privacy = :publicPrivacy)
              )
            ORDER BY p.createdAt DESC
            """)
    Page<Post> findVisiblePetPosts(
            @Param("petId") Long petId,
            @Param("currentUserId") Long currentUserId,
            @Param("deletedStatus") PostStatus deletedStatus,
            @Param("publishedStatus") PostStatus publishedStatus,
            @Param("publicPrivacy") PostPrivacy publicPrivacy,
            Pageable pageable
    );
}
