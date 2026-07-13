package com.petcare.backend.service.impl;

import com.petcare.backend.dto.blog.request.AdminCreateBlogRequest;
import com.petcare.backend.dto.blog.request.AdminUpdateBlogRequest;
import com.petcare.backend.dto.blog.response.BlogOptionResponse;
import com.petcare.backend.dto.blog.response.BlogResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Blog;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.BlogRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AdminBlogService;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminBlogServiceImpl implements AdminBlogService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    @Override
    public List<BlogOptionResponse> getBlogStatuses() {
        return List.of(
                option(Blog.BlogStatus.draft, "Bản nháp"),
                option(Blog.BlogStatus.published, "Đã xuất bản"),
                option(Blog.BlogStatus.archived, "Đã lưu trữ")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogResponse> getBlogs(
            String keyword,
            Blog.BlogCategory category,
            Blog.BlogStatus status,
            int page,
            int size) {
        Page<Blog> blogs = blogRepository.searchAdmin(
                normalizeKeyword(keyword),
                category,
                status,
                buildPageable(page, size)
        );
        List<BlogResponse> content = blogs.getContent().stream()
                .map(blog -> BlogResponse.from(blog, false, false))
                .toList();
        return toPageResponse(blogs, content);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogResponse getBlogDetail(Long blogId) {
        return BlogResponse.from(getBlog(blogId), true, false);
    }

    @Override
    @Transactional
    public BlogResponse createBlog(UserPrincipal principal, AdminCreateBlogRequest request) {
        Blog blog = new Blog();
        blog.setAuthor(getUser(principal.getId()));
        blog.setTitle(trimRequired(request.getTitle(), "Tiêu đề không được để trống"));
        blog.setSlug(uniqueSlug(slugSource(request.getSlug(), request.getTitle()), null));
        blog.setSummary(trimToNull(request.getSummary()));
        blog.setContent(trimRequired(request.getContent(), "Nội dung không được để trống"));
        blog.setCoverImageUrl(trimToNull(request.getCoverImageUrl()));
        blog.setCategory(request.getCategory());
        blog.setStatus(request.getStatus());
        blog.setReadTimeMinutes(readTime(request.getReadTimeMinutes()));
        if (request.getStatus() == Blog.BlogStatus.published) {
            blog.setPublishedAt(LocalDateTime.now());
        }
        return BlogResponse.from(blogRepository.save(blog), true, false);
    }

    @Override
    @Transactional
    public BlogResponse updateBlog(Long blogId, AdminUpdateBlogRequest request) {
        Blog blog = getBlog(blogId);
        if (request.getTitle() != null) {
            blog.setTitle(trimRequired(request.getTitle(), "Tiêu đề không được để trống"));
        }
        if (request.getSlug() != null) {
            blog.setSlug(uniqueSlug(request.getSlug(), blog.getId()));
        }
        if (request.getSummary() != null) {
            blog.setSummary(trimToNull(request.getSummary()));
        }
        if (request.getContent() != null) {
            blog.setContent(trimRequired(request.getContent(), "Nội dung không được để trống"));
        }
        if (request.getCoverImageUrl() != null) {
            blog.setCoverImageUrl(trimToNull(request.getCoverImageUrl()));
        }
        if (request.getCategory() != null) {
            blog.setCategory(request.getCategory());
        }
        if (request.getReadTimeMinutes() != null) {
            blog.setReadTimeMinutes(readTime(request.getReadTimeMinutes()));
        }
        if (request.getStatus() != null) {
            Blog.BlogStatus oldStatus = blog.getStatus();
            blog.setStatus(request.getStatus());
            if (oldStatus != Blog.BlogStatus.published
                    && request.getStatus() == Blog.BlogStatus.published
                    && blog.getPublishedAt() == null) {
                blog.setPublishedAt(LocalDateTime.now());
            }
        }
        return BlogResponse.from(blogRepository.save(blog), true, false);
    }

    private Blog getBlog(Long blogId) {
        if (blogId == null || blogId <= 0) {
            throw new BadRequestException("Blog không hợp lệ");
        }
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blog"));
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

    private String trimRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Integer readTime(Integer value) {
        return value == null ? 1 : Math.max(1, value);
    }

    private String slugSource(String requestedSlug, String title) {
        return StringUtils.hasText(requestedSlug) ? requestedSlug : title;
    }

    private String uniqueSlug(String source, Long currentBlogId) {
        String base = slugify(source);
        String candidate = base;
        int index = 2;
        while (blogRepository.existsBySlug(candidate)
                && !isCurrentBlogSlug(candidate, currentBlogId)) {
            candidate = base + "-" + index;
            index++;
        }
        return candidate;
    }

    private boolean isCurrentBlogSlug(String slug, Long currentBlogId) {
        return currentBlogId != null
                && blogRepository.findById(currentBlogId)
                        .map(blog -> slug.equals(blog.getSlug()))
                        .orElse(false);
    }

    private String slugify(String source) {
        if (!StringUtils.hasText(source)) {
            throw new BadRequestException("Slug không hợp lệ");
        }
        String normalized = Normalizer.normalize(source.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(normalized)
                .replaceAll("")
                .replace("đ", "d");
        String slug = withoutDiacritics
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (!StringUtils.hasText(slug)) {
            throw new BadRequestException("Slug không hợp lệ");
        }
        return slug.length() > 220 ? slug.substring(0, 220).replaceAll("-$", "") : slug;
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

    private BlogOptionResponse option(Blog.BlogStatus value, String label) {
        return BlogOptionResponse.builder()
                .value(value.name())
                .label(label)
                .build();
    }
}
