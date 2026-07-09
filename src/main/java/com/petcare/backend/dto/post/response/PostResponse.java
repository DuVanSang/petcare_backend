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
public class PostResponse {
    private Long id;
    private Long userId;
    private String authorName;
    private String authorAvatarUrl;
    private List<PetSummaryResponse> pets;
    private String caption;
    private String privacy;
    private String status;
    private Boolean commentsLocked;
    private List<PostMediaResponse> media;
    private List<PostCommentResponse> comments;
    private ReactionSummaryResponse reactions;
    private long commentCount;
    private boolean reactedByCurrentUser;
    private String currentUserReaction;
    private Boolean savedByCurrentUser;
    private boolean canEdit;
    private boolean canDelete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
