package com.petcare.backend.controller;

import com.petcare.backend.dto.blog.request.AdminCreateBlogRequest;
import com.petcare.backend.dto.blog.request.AdminUpdateBlogRequest;
import com.petcare.backend.dto.blog.response.BlogOptionResponse;
import com.petcare.backend.dto.blog.response.BlogResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.model.Blog;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AdminBlogService;
import com.petcare.backend.service.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/blogs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@Tag(name = "Admin - Blogs", description = "Quản lý blog kiến thức")
@SecurityRequirement(name = "bearerAuth")
public class AdminBlogController {
    private final AdminBlogService adminBlogService;
    private final BlogService blogService;

    @GetMapping("/categories")
    @Operation(summary = "Lấy danh sách danh mục blog")
    public ResponseEntity<ApiResponse<List<BlogOptionResponse>>> getBlogCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách danh mục blog thành công",
                blogService.getBlogCategories()
        ));
    }

    @GetMapping("/statuses")
    @Operation(summary = "Lấy danh sách trạng thái blog")
    public ResponseEntity<ApiResponse<List<BlogOptionResponse>>> getBlogStatuses() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách trạng thái blog thành công",
                adminBlogService.getBlogStatuses()
        ));
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm và lọc danh sách blog")
    public ResponseEntity<ApiResponse<PageResponse<BlogResponse>>> getBlogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Blog.BlogCategory category,
            @RequestParam(required = false) Blog.BlogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách blog thành công",
                adminBlogService.getBlogs(keyword, category, status, page, size)
        ));
    }

    @GetMapping("/{blogId}")
    @Operation(summary = "Xem chi tiết blog")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogDetail(@PathVariable Long blogId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết blog thành công",
                adminBlogService.getBlogDetail(blogId)
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo blog")
    public ResponseEntity<ApiResponse<BlogResponse>> createBlog(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdminCreateBlogRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo blog thành công",
                adminBlogService.createBlog(principal, request)
        ));
    }

    @PatchMapping("/{blogId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật blog")
    public ResponseEntity<ApiResponse<BlogResponse>> updateBlog(
            @PathVariable Long blogId,
            @Valid @RequestBody AdminUpdateBlogRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật blog thành công",
                adminBlogService.updateBlog(blogId, request)
        ));
    }
}
