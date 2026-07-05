package com.petcare.backend.service;

import com.petcare.backend.dto.blog.response.BlogResponse;
import com.petcare.backend.dto.blog.response.BlogOptionResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.model.Blog;
import com.petcare.backend.security.UserPrincipal;
import java.util.List;

public interface BlogService {
    List<BlogOptionResponse> getBlogCategories();

    PageResponse<BlogResponse> getPublishedBlogs(
            UserPrincipal principal,
            String keyword,
            Blog.BlogCategory category,
            int page,
            int size
    );

    BlogResponse getPublishedBlogDetail(UserPrincipal principal, Long blogId);

    BlogResponse saveBlog(UserPrincipal principal, Long blogId);

    void unsaveBlog(UserPrincipal principal, Long blogId);

    PageResponse<BlogResponse> getSavedBlogs(UserPrincipal principal, int page, int size);
}
