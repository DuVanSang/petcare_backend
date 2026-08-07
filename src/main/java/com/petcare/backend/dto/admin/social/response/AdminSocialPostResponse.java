package com.petcare.backend.dto.admin.social.response;

import com.petcare.backend.model.Post;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminSocialPostResponse {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private Long petId;
    private String petName;
    private String caption;
    private String privacy;
    private String status;
    private Boolean commentsLocked;
    private long reactionCount;
    private long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminSocialPostResponse from(Post post, long reactionCount, long commentCount, String petName) {
        return AdminSocialPostResponse.builder()
                .id(post.getId())
                .authorId(post.getUser() == null ? null : post.getUser().getId())
                .authorName(post.getUser() == null ? null : post.getUser().getFullName())
                .authorEmail(post.getUser() == null ? null : post.getUser().getEmail())
                .petId(post.getPetId())
                .petName(petName)
                .caption(post.getCaption())
                .privacy(post.getPrivacy() == null ? null : post.getPrivacy().getValue())
                .status(post.getStatus() == null ? null : post.getStatus().getValue())
                .commentsLocked(post.getCommentsLocked())
                .reactionCount(reactionCount)
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
