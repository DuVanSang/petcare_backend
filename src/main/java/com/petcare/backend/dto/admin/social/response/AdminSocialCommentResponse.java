package com.petcare.backend.dto.admin.social.response;

import com.petcare.backend.model.PostComment;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminSocialCommentResponse {
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private Long parentCommentId;
    private Long rootCommentId;
    private Integer depth;
    private String commentText;
    private String status;
    private long reactionCount;
    private long replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminSocialCommentResponse from(PostComment comment, long reactionCount, long replyCount) {
        return AdminSocialCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost() == null ? null : comment.getPost().getId())
                .authorId(comment.getUser() == null ? null : comment.getUser().getId())
                .authorName(comment.getUser() == null ? null : comment.getUser().getFullName())
                .authorEmail(comment.getUser() == null ? null : comment.getUser().getEmail())
                .parentCommentId(comment.getParentCommentId())
                .rootCommentId(comment.getRootCommentId())
                .depth(comment.getDepth())
                .commentText(comment.getCommentText())
                .status(comment.getStatus() == null ? null : comment.getStatus().getValue())
                .reactionCount(reactionCount)
                .replyCount(replyCount)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
