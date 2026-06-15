package com.petcare.backend.dto.post.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {
    private Long parentCommentId;

    @NotBlank(message = "Comment text is required")
    @Size(max = 2000, message = "Comment text must not exceed 2000 characters")
    private String commentText;
}
