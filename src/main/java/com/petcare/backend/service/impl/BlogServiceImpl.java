package com.petcare.backend.service.impl;

import com.petcare.backend.dto.blog.response.BlogOptionResponse;
import com.petcare.backend.dto.blog.response.BlogResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Blog;
import com.petcare.backend.model.BlogSave;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.BlogRepository;
import com.petcare.backend.repository.BlogSaveRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.BlogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private static final int MAX_PAGE_SIZE = 50;

    private final BlogRepository blogRepository;
    private final BlogSaveRepository blogSaveRepository;
    private final UserRepository userRepository;

    @Override
    public List<BlogOptionResponse> getBlogCategories() {
        return List.of(
                option(Blog.BlogCategory.health, "Sức khỏe"),
                option(Blog.BlogCategory.nutrition, "Dinh dưỡng"),
                option(Blog.BlogCategory.training, "Huấn luyện"),
                option(Blog.BlogCategory.grooming, "Chăm sóc lông"),
                option(Blog.BlogCategory.vaccination, "Tiêm phòng")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> getPublishedBlogs(
            UserPrincipal principal,
            String keyword,
            Blog.BlogCategory category,
            int page,
            int size) {
        Long currentUserId = principal.getId();
        Page<Blog> blogs = blogRepository.searchPublished(
                Blog.BlogStatus.published,
                normalizeKeyword(keyword),
                category,
                buildPageable(page, size)
        );
        List<BlogResponse> content = blogs.getContent().stream()
                .map(blog -> BlogResponse.from(blog, false, isSaved(blog.getId(), currentUserId)))
                .toList();
        return toPageResponse(blogs, content);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogResponse getPublishedBlogDetail(UserPrincipal principal, Long blogId) {
        Blog blog = blogRepository.findByIdAndStatus(blogId, Blog.BlogStatus.published)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blog"));
        return BlogResponse.from(blog, true, isSaved(blog.getId(), principal.getId()));
    }

    @Override
    @Transactional
    public BlogResponse saveBlog(UserPrincipal principal, Long blogId) {
        Blog blog = blogRepository.findByIdAndStatus(blogId, Blog.BlogStatus.published)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blog"));
        if (!blogSaveRepository.existsByBlog_IdAndUser_Id(blogId, principal.getId())) {
            BlogSave blogSave = new BlogSave();
            blogSave.setBlog(blog);
            blogSave.setUser(getUser(principal.getId()));
            blogSaveRepository.save(blogSave);
        }
        return BlogResponse.from(blog, true, true);
    }

    @Override
    @Transactional
    public void unsaveBlog(UserPrincipal principal, Long blogId) {
        blogSaveRepository.findByBlog_IdAndUser_Id(blogId, principal.getId())
                .ifPresent(blogSaveRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> getSavedBlogs(UserPrincipal principal, int page, int size) {
        Page<BlogSave> savedBlogs = blogSaveRepository.findByUser_IdOrderByCreatedAtDesc(
                principal.getId(),
                buildPageable(page, size)
        );
        List<BlogResponse> content = savedBlogs.getContent().stream()
                .map(saved -> BlogResponse.from(saved.getBlog(), false, true))
                .toList();
        return PageResponse.<BlogResponse>builder()
                .content(content)
                .page(savedBlogs.getNumber())
                .size(savedBlogs.getSize())
                .totalElements(savedBlogs.getTotalElements())
                .totalPages(savedBlogs.getTotalPages())
                .first(savedBlogs.isFirst())
                .last(savedBlogs.isLast())
                .build();
    }

    private boolean isSaved(Long blogId, Long currentUserId) {
        return currentUserId != null && blogSaveRepository.existsByBlog_IdAndUser_Id(blogId, currentUserId);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private Pageable buildPageable(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Số trang không hợp lệ");
        }
        if (size <= 0) {
            throw new BadRequestException("Kích thước trang phải lớn hơn 0");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private PageResponse<BlogResponse> toPageResponse(Page<Blog> page, List<BlogResponse> content) {
        return PageResponse.<BlogResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private BlogOptionResponse option(Blog.BlogCategory value, String label) {
        return BlogOptionResponse.builder()
                .value(value.name())
                .label(label)
                .build();
    }
}
