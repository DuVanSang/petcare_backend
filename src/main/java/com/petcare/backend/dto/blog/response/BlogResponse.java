package com.petcare.backend.dto.blog.response;

import com.petcare.backend.model.Blog;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BlogResponse {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String coverImageUrl;
    private String category;
    private String status;
    private Integer readTimeMinutes;
    private Long authorId;
    private String authorName;
    private Boolean savedByCurrentUser;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BlogResponse from(Blog blog, boolean includeContent, boolean savedByCurrentUser) {
        return BlogResponse.builder()
                .id(blog.getId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .summary(blog.getSummary())
                .content(includeContent ? blog.getContent() : null)
                .coverImageUrl(blog.getCoverImageUrl())
                .category(blog.getCategory() == null ? null : blog.getCategory().name())
                .status(blog.getStatus() == null ? null : blog.getStatus().name())
                .readTimeMinutes(blog.getReadTimeMinutes())
                .authorId(blog.getAuthor() == null ? null : blog.getAuthor().getId())
                .authorName(blog.getAuthor() == null ? null : blog.getAuthor().getFullName())
                .savedByCurrentUser(savedByCurrentUser)
                .publishedAt(blog.getPublishedAt())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }
}
