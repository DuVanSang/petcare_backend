package com.petcare.backend.service;

import com.petcare.backend.dto.post.response.ReactionSummaryResponse;

public interface ReactionService {
    ReactionSummaryResponse reactToPost(Long postId, String reactionType, Long currentUserId);

    ReactionSummaryResponse removePostReaction(Long postId, Long currentUserId);

    ReactionSummaryResponse getPostReactionSummary(Long postId, Long currentUserId);

    ReactionSummaryResponse reactToComment(Long commentId, String reactionType, Long currentUserId);

    ReactionSummaryResponse removeCommentReaction(Long commentId, Long currentUserId);

    ReactionSummaryResponse getCommentReactionSummary(Long commentId, Long currentUserId);
}
