package com.petcare.backend.repository;

import com.petcare.backend.model.CommentReaction;
import com.petcare.backend.model.CommentReactionId;
import com.petcare.backend.model.enums.ReactionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, CommentReactionId> {
    Optional<CommentReaction> findByComment_IdAndUser_Id(Long commentId, Long userId);

    boolean existsByComment_IdAndUser_Id(Long commentId, Long userId);

    long countByComment_Id(Long commentId);

    long countByComment_IdAndReactionType(Long commentId, ReactionType reactionType);

    void deleteByComment_Post_Id(Long postId);

    void deleteByComment_IdIn(List<Long> commentIds);

    void deleteByComment_IdAndUser_Id(Long commentId, Long userId);
}
