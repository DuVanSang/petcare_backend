package com.petcare.backend.dto.post.response;

import java.time.LocalDateTime;
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
public class CommentMediaResponse {
    private Long id;
    private String mediaType;
    private String mediaUrl;
    private String thumbnailUrl;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private Integer displayOrder;
    private String altText;
    private LocalDateTime createdAt;
}
