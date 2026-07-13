package com.petcare.backend.controller;

import com.petcare.backend.dto.blog.response.BlogOptionResponse;
import com.petcare.backend.dto.blog.response.BlogResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.model.Blog;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blogs")
@RequiredArgsConstructor
@Tag(name = "Blogs", description = "Blog kiến thức do admin đăng")
@SecurityRequirement(name = "bearerAuth")
public class BlogController {
    private final BlogService blogService;

    @GetMapping("/categories")
    @Operation(summary = "Lấy danh sách danh mục blog")
    public ResponseEntity<ApiResponse<List<BlogOptionResponse>>> getBlogCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách danh mục blog thành công",
                blogService.getBlogCategories()
        ));
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm danh sách blog đã xuất bản")
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> getPublishedBlogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Blog.BlogCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách blog thành công",
                blogService.getPublishedBlogs(principal, keyword, category, page, size)
        ));
    }

    @GetMapping("/saved")
    @Operation(summary = "Lấy danh sách blog đã lưu")
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> getSavedBlogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách blog đã lưu thành công",
                blogService.getSavedBlogs(principal, page, size)
        ));
    }

    @GetMapping("/{blogId}")
    @Operation(summary = "Xem chi tiết blog")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long blogId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết blog thành công",
                blogService.getPublishedBlogDetail(principal, blogId)
        ));
    }

    @PostMapping("/{blogId}/save")
    @Operation(summary = "Lưu blog")
    public ResponseEntity<ApiResponse<BlogResponse>> saveBlog(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long blogId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lưu blog thành công",
                blogService.saveBlog(principal, blogId)
        ));
    }

    @DeleteMapping("/{blogId}/save")
    @Operation(summary = "Bỏ lưu blog")
    public ResponseEntity<ApiResponse<Void>> unsaveBlog(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long blogId
    ) {
        blogService.unsaveBlog(principal, blogId);
        return ResponseEntity.ok(ApiResponse.success("Bỏ lưu blog thành công", null));
    }
}
