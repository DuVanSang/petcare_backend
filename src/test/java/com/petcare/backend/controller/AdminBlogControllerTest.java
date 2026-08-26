package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.petcare.backend.dto.blog.request.AdminCreateBlogRequest;
import com.petcare.backend.dto.blog.request.AdminUpdateBlogRequest;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.model.Blog;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AdminBlogService;
import com.petcare.backend.service.BlogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminBlogControllerTest {
    @Mock private AdminBlogService adminService;
    @Mock private BlogService blogService;
    @Mock private UserPrincipal principal;
    @InjectMocks private AdminBlogController controller;

    @Test
    void queryEndpoints_ReturnSuccessAndDelegateFilters() {
        assertOk(controller.getBlogCategories());
        assertOk(controller.getBlogStatuses());
        Blog.BlogCategory category = Blog.BlogCategory.values()[0];
        Blog.BlogStatus status = Blog.BlogStatus.values()[0];
        assertOk(controller.getBlogs("pet", category, status, 0, 1));
        assertOk(controller.getBlogDetail(10L));
        verify(blogService).getBlogCategories();
        verify(adminService).getBlogStatuses();
        verify(adminService).getBlogs("pet", category, status, 0, 1);
        verify(adminService).getBlogDetail(10L);
    }

    @Test
    void mutationEndpoints_ReturnCreatedOrOkAndDelegateRequests() {
        assertThat(controller.createBlog(principal, new AdminCreateBlogRequest()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertOk(controller.updateBlog(11L, new AdminUpdateBlogRequest()));
        verify(adminService).createBlog(org.mockito.ArgumentMatchers.eq(principal), org.mockito.ArgumentMatchers.any(AdminCreateBlogRequest.class));
        verify(adminService).updateBlog(org.mockito.ArgumentMatchers.eq(11L), org.mockito.ArgumentMatchers.any(AdminUpdateBlogRequest.class));
    }

    private void assertOk(ResponseEntity<? extends ApiResponse<?>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
