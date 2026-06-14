package com.petcare.backend.dto.post.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentReactionRequest {
    @NotBlank(message = "Reaction type is required")
    private String reactionType;
}
