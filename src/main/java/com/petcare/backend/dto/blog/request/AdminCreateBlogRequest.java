package com.petcare.backend.dto.blog.request;

import com.petcare.backend.model.Blog;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateBlogRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String title;

    @Size(max = 220, message = "Slug không được vượt quá 220 ký tự")
    private String slug;

    @Size(max = 500, message = "Tóm tắt không được vượt quá 500 ký tự")
    private String summary;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    @Size(max = 500, message = "URL ảnh bìa không được vượt quá 500 ký tự")
    private String coverImageUrl;

    @NotNull(message = "Vui lòng chọn danh mục blog")
    private Blog.BlogCategory category;

    @NotNull(message = "Vui lòng chọn trạng thái blog")
    private Blog.BlogStatus status;

    @Min(value = 1, message = "Thời gian đọc phải từ 1 phút trở lên")
    private Integer readTimeMinutes;
}
