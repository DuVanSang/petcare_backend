package com.petcare.backend.dto.post.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePostRequest {
    private Long petId;

    @Size(max = 3000, message = "Caption must not exceed 3000 characters")
    private String caption;

    @NotBlank(message = "Privacy is required")
    private String privacy;

    @Valid
    @Size(max = 10, message = "A post can contain at most 10 media items")
    private List<CreatePostMediaRequest> media;
}
