package com.petcare.backend.service.impl;

import com.petcare.backend.dto.post.response.CommentMediaResponse;
import com.petcare.backend.dto.post.response.PetSummaryResponse;
import com.petcare.backend.dto.post.response.PostCommentResponse;
import com.petcare.backend.dto.post.response.PostMediaResponse;
import com.petcare.backend.dto.post.response.PostResponse;
import com.petcare.backend.dto.post.response.ReactionSummaryResponse;
import com.petcare.backend.model.CommentMedia;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.PostMedia;
import com.petcare.backend.model.User;
import com.petcare.backend.service.PostMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PostMapperImpl implements PostMapper {
    @Override
    public PostResponse toPostResponse(
            Post post,
            List<PostMedia> media,
            List<PetSummaryResponse> pets,
            ReactionSummaryResponse reactions,
            long commentCount,
            Long currentUserId
    ) {
        User author = post.getUser();
        Long authorId = author == null ? null : author.getId();
        String currentUserReaction = reactions == null ? null : reactions.getCurrentUserReaction();
        boolean owner = currentUserId != null && currentUserId.equals(authorId);

        return PostResponse.builder()
                .id(post.getId())
                .userId(authorId)
                .authorName(resolveAuthorName(author))
                .authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .pets(pets == null ? Collections.emptyList() : pets)
                .caption(post.getCaption())
                .privacy(post.getPrivacy() == null ? null : post.getPrivacy().getValue())
                .status(post.getStatus() == null ? null : post.getStatus().getValue())
                .commentsLocked(post.getCommentsLocked())
                .media(toPostMediaResponses(media))
                .comments(Collections.emptyList())
                .reactions(reactions == null ? ReactionSummaryResponse.empty() : reactions)
                .commentCount(commentCount)
                .reactedByCurrentUser(currentUserReaction != null)
                .currentUserReaction(currentUserReaction)
                .canEdit(owner)
                .canDelete(owner)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    @Override
    public PostMediaResponse toPostMediaResponse(PostMedia media) {
        return PostMediaResponse.builder()
                .id(media.getId())
                .mediaType(media.getMediaType() == null ? null : media.getMediaType().getValue())
                .mediaUrl(media.getMediaUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .originalFilename(media.getOriginalFilename())
                .mimeType(media.getMimeType())
                .fileSize(media.getFileSize())
                .displayOrder(media.getDisplayOrder())
                .altText(media.getAltText())
                .createdAt(media.getCreatedAt())
                .build();
    }

    @Override
    public CommentMediaResponse toCommentMediaResponse(CommentMedia media) {
        return CommentMediaResponse.builder()
                .id(media.getId())
                .mediaType(media.getMediaType() == null ? null : media.getMediaType().getValue())
                .mediaUrl(media.getMediaUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .originalFilename(media.getOriginalFilename())
                .mimeType(media.getMimeType())
                .fileSize(media.getFileSize())
                .displayOrder(media.getDisplayOrder())
                .altText(media.getAltText())
                .createdAt(media.getCreatedAt())
                .build();
    }

    @Override
    public PostCommentResponse toCommentResponse(
            PostComment comment,
            Long postOwnerId,
            Long currentUserId
    ) {
        return toCommentResponse(
                comment,
                Collections.emptyList(),
                ReactionSummaryResponse.empty(),
                0L,
                postOwnerId,
                currentUserId
        );
    }

    @Override
    public PostCommentResponse toCommentResponse(
            PostComment comment,
            List<CommentMedia> media,
            ReactionSummaryResponse reactions,
            long replyCount,
            Long postOwnerId,
            Long currentUserId
    ) {
        User author = comment.getUser();
        Long authorId = author == null ? null : author.getId();
        String currentUserReaction = reactions == null ? null : reactions.getCurrentUserReaction();
        boolean canDelete = currentUserId != null
                && (currentUserId.equals(authorId) || currentUserId.equals(postOwnerId));

        return PostCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost() == null ? null : comment.getPost().getId())
                .userId(authorId)
                .authorName(resolveAuthorName(author))
                .authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .parentCommentId(comment.getParentCommentId())
                .rootCommentId(comment.getRootCommentId())
                .depth(comment.getDepth())
                .commentText(comment.getCommentText())
                .status(comment.getStatus() == null ? null : comment.getStatus().getValue())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .canDelete(canDelete)
                .media(toCommentMediaResponses(media))
                .replies(Collections.emptyList())
                .reactions(reactions == null ? ReactionSummaryResponse.empty() : reactions)
                .replyCount(replyCount)
                .reactedByCurrentUser(currentUserReaction != null)
                .currentUserReaction(currentUserReaction)
                .build();
    }

    private List<PostMediaResponse> toPostMediaResponses(List<PostMedia> media) {
        if (media == null || media.isEmpty()) {
            return Collections.emptyList();
        }
        return media.stream()
                .map(this::toPostMediaResponse)
                .toList();
    }

    private List<CommentMediaResponse> toCommentMediaResponses(List<CommentMedia> media) {
        if (media == null || media.isEmpty()) {
            return Collections.emptyList();
        }
        return media.stream()
                .map(this::toCommentMediaResponse)
                .toList();
    }

    private String resolveAuthorName(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getFullName())) {
            return user.getFullName();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return "User " + user.getId();
    }
}
