package com.petcare.backend.dto.post.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePostMediaRequest {
    private String mediaType;

    @NotBlank(message = "Media URL is required")
    @Size(max = 500, message = "Media URL must not exceed 500 characters")
    private String mediaUrl;

    @Size(max = 500, message = "Thumbnail URL must not exceed 500 characters")
    private String thumbnailUrl;

    @Size(max = 255, message = "Original filename must not exceed 255 characters")
    private String originalFilename;

    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    private String mimeType;

    @Min(value = 0, message = "File size must not be negative")
    private Long fileSize;

    @Min(value = 0, message = "Display order must not be negative")
    private Integer displayOrder;

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    private String altText;
}
