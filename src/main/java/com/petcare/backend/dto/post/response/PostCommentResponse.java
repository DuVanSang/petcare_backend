package com.petcare.backend.dto.post.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCommentResponse {
    private Long id;
    private Long postId;
    private Long userId;
    private String authorName;
    private String authorAvatarUrl;
    private Long parentCommentId;
    private Long rootCommentId;
    private Integer depth;
    private String commentText;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canDelete;
    private List<CommentMediaResponse> media;
    private List<PostCommentResponse> replies;
    private ReactionSummaryResponse reactions;
    private long replyCount;
    private boolean reactedByCurrentUser;
    private String currentUserReaction;
}
