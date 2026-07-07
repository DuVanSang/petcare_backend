package com.petcare.backend.service;

import com.petcare.backend.dto.post.response.PostCommentResponse;
import com.petcare.backend.dto.post.response.CommentMediaResponse;
import com.petcare.backend.dto.post.response.PetSummaryResponse;
import com.petcare.backend.dto.post.response.PostMediaResponse;
import com.petcare.backend.dto.post.response.PostResponse;
import com.petcare.backend.dto.post.response.ReactionSummaryResponse;
import com.petcare.backend.model.CommentMedia;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.PostMedia;
import java.util.List;

public interface PostMapper {
    PostResponse toPostResponse(
            Post post,
            List<PostMedia> media,
            List<PetSummaryResponse> pets,
            ReactionSummaryResponse reactions,
            long commentCount,
            Long currentUserId
    );

    PostMediaResponse toPostMediaResponse(PostMedia media);

    CommentMediaResponse toCommentMediaResponse(CommentMedia media);

    PostCommentResponse toCommentResponse(
            PostComment comment,
            Long postOwnerId,
            Long currentUserId
    );

    PostCommentResponse toCommentResponse(
            PostComment comment,
            List<CommentMedia> media,
            ReactionSummaryResponse reactions,
            long replyCount,
            Long postOwnerId,
            Long currentUserId
    );
}
