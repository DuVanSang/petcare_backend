package com.petcare.backend.repository;

import com.petcare.backend.model.PostSave;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostSaveRepository extends JpaRepository<PostSave, Long> {
    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    Optional<PostSave> findByPost_IdAndUser_Id(Long postId, Long userId);

    List<PostSave> findByUser_IdAndPost_IdIn(Long userId, Collection<Long> postIds);

    Page<PostSave> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT ps
            FROM PostSave ps
            WHERE ps.user.id = :currentUserId
              AND ps.post.status = :publishedStatus
              AND (
                    ps.post.user.id = :currentUserId
                    OR ps.post.privacy = :publicPrivacy
                    OR (
                        ps.post.privacy = :friendsPrivacy
                        AND EXISTS (
                            SELECT f
                            FROM Friendship f
                            WHERE (f.user1.id = :currentUserId AND f.user2.id = ps.post.user.id)
                               OR (f.user1.id = ps.post.user.id AND f.user2.id = :currentUserId)
                        )
                    )
              )
            ORDER BY ps.createdAt DESC
            """)
    Page<PostSave> findVisibleSavedPosts(
            @Param("currentUserId") Long currentUserId,
            @Param("publishedStatus") PostStatus publishedStatus,
            @Param("publicPrivacy") PostPrivacy publicPrivacy,
            @Param("friendsPrivacy") PostPrivacy friendsPrivacy,
            Pageable pageable
    );
}
