package com.petcare.backend.repository;

import com.petcare.backend.model.PostReaction;
import com.petcare.backend.model.PostReactionId;
import com.petcare.backend.model.enums.ReactionType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReactionRepository extends JpaRepository<PostReaction, PostReactionId> {
    Optional<PostReaction> findByPost_IdAndUser_Id(Long postId, Long userId);

    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    long countByPost_Id(Long postId);

    long countByPost_IdAndReactionType(Long postId, ReactionType reactionType);

    void deleteByPost_Id(Long postId);

    void deleteByPost_IdAndUser_Id(Long postId, Long userId);
}
