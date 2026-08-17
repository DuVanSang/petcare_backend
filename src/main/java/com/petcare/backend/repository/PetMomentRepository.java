package com.petcare.backend.repository;

import com.petcare.backend.model.PetMoment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PetMomentRepository extends JpaRepository<PetMoment, Long> {

    List<PetMoment> findByPetIdOrderByCreatedAtDesc(Long petId);

    @Query("""
        SELECT DISTINCT m FROM PetMoment m
        JOIN FETCH m.pet p
        JOIN FETCH m.user u
        WHERE m.createdAt >= :since
        AND (
            p.owner.id = :userId
            OR p.id IN (SELECT cp.pet.id FROM PetCoParent cp WHERE cp.user.id = :userId)
            OR (
                (m.audience IS NULL OR m.audience != 'CO_PARENTS')
                AND p.owner.id IN (
                    SELECT CASE WHEN f.user1.id = :userId THEN f.user2.id ELSE f.user1.id END
                    FROM Friendship f
                    WHERE f.user1.id = :userId OR f.user2.id = :userId
                )
            )
        )
        ORDER BY m.createdAt DESC
    """)
    List<PetMoment> findActiveFeedMoments(
            @Param("userId") Long userId,
            @Param("since") Instant since
    );
}
