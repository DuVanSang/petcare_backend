package com.petcare.backend.service;

import com.petcare.backend.dto.blog.request.AdminCreateBlogRequest;
import com.petcare.backend.dto.blog.request.AdminUpdateBlogRequest;
import com.petcare.backend.dto.blog.response.BlogOptionResponse;
import com.petcare.backend.dto.blog.response.BlogResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.model.Blog;
import com.petcare.backend.security.UserPrincipal;
import java.util.List;

public interface AdminBlogService {
    List<BlogOptionResponse> getBlogStatuses();

    PageResponse<BlogResponse> getBlogs(
            String keyword,
            Blog.BlogCategory category,
            Blog.BlogStatus status,
            int page,
            int size
    );

    BlogResponse getBlogDetail(Long blogId);

    BlogResponse createBlog(UserPrincipal principal, AdminCreateBlogRequest request);

    BlogResponse updateBlog(Long blogId, AdminUpdateBlogRequest request);
}
