package com.petcare.backend.repository;

import com.petcare.backend.model.PostMedia;
import com.petcare.backend.model.enums.MediaType;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    List<PostMedia> findByPost_IdOrderByDisplayOrderAsc(Long postId);

    void deleteByPost_Id(Long postId);

    @Query("""
            SELECT pm
            FROM PostMedia pm
            JOIN pm.post p
            WHERE p.petId = :petId
              AND p.status = :publishedStatus
              AND pm.mediaType = :imageType
              AND (
                    p.user.id = :currentUserId
                    OR p.privacy = :publicPrivacy
                    OR (
                        p.privacy = :friendsPrivacy
                        AND EXISTS (
                            SELECT f
                            FROM Friendship f
                            WHERE (f.user1.id = :currentUserId AND f.user2.id = p.user.id)
                               OR (f.user1.id = p.user.id AND f.user2.id = :currentUserId)
                        )
                    )
              )
            ORDER BY p.createdAt DESC, pm.displayOrder ASC, pm.id ASC
            """)
    Page<PostMedia> findVisiblePetAlbumImages(
            @Param("petId") Long petId,
            @Param("currentUserId") Long currentUserId,
            @Param("publishedStatus") PostStatus publishedStatus,
            @Param("imageType") MediaType imageType,
            @Param("publicPrivacy") PostPrivacy publicPrivacy,
            @Param("friendsPrivacy") PostPrivacy friendsPrivacy,
            Pageable pageable
    );
}
