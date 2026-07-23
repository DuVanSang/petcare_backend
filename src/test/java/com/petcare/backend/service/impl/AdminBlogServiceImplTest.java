package com.petcare.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.blog.request.AdminCreateBlogRequest;
import com.petcare.backend.dto.blog.request.AdminUpdateBlogRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Blog;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.BlogRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminBlogServiceImplTest {
    @Mock private BlogRepository blogs;
    @Mock private UserRepository users;
    @Mock private UserPrincipal principal;
    private AdminBlogServiceImpl service;

    @BeforeEach void setUp() { service = new AdminBlogServiceImpl(blogs, users); when(principal.getId()).thenReturn(1L); }

    @Test
    void statusesAndListMapDataFilterTrimAndPaginationBoundary() {
        assertEquals(List.of("draft", "published", "archived"), service.getBlogStatuses().stream().map(option -> option.getValue()).toList());
        Blog blog = blog(2L, Blog.BlogStatus.published); when(blogs.searchAdmin(eq("dog"), eq(Blog.BlogCategory.health), eq(Blog.BlogStatus.published), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(blog)));
        var page = service.getBlogs(" dog ", Blog.BlogCategory.health, Blog.BlogStatus.published, 0, 99);
        assertEquals(1, page.getContent().size()); assertEquals("Title", page.getContent().getFirst().getTitle()); assertNull(page.getContent().getFirst().getContent());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class); verify(blogs).searchAdmin(eq("dog"), eq(Blog.BlogCategory.health), eq(Blog.BlogStatus.published), pageable.capture()); assertEquals(50, pageable.getValue().getPageSize());
        when(blogs.searchAdmin(eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(new PageImpl<>(List.of())); assertEquals(0, service.getBlogs(" ", null, null, 0, 1).getContent().size());
        assertThrows(BadRequestException.class, () -> service.getBlogs(null, null, null, -1, 1)); assertThrows(BadRequestException.class, () -> service.getBlogs(null, null, null, 0, 0));
    }

    @Test
    void getDetailReturnsFullContentAndRejectsInvalidOrMissingIds() {
        Blog blog = blog(2L, Blog.BlogStatus.draft); when(blogs.findById(2L)).thenReturn(Optional.of(blog));
        var response = service.getBlogDetail(2L); assertEquals("Content", response.getContent()); assertEquals("draft", response.getStatus());
        assertThrows(BadRequestException.class, () -> service.getBlogDetail(null)); assertThrows(BadRequestException.class, () -> service.getBlogDetail(0L)); assertThrows(BadRequestException.class, () -> service.getBlogDetail(-1L));
        when(blogs.findById(9L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.getBlogDetail(9L));
    }

    @Test
    void createDraftMapsAuthorTrimsFieldsAndUsesTitleSlugWhenRequestedSlugAbsent() {
        User author = user(1L); when(users.findById(1L)).thenReturn(Optional.of(author)); when(blogs.save(any(Blog.class))).thenAnswer(invocation -> { Blog saved = invocation.getArgument(0); saved.setId(10L); return saved; });
        AdminCreateBlogRequest request = createRequest("  Chăm sóc Động vật  ", null, Blog.BlogStatus.draft); request.setSummary(" "); request.setCoverImageUrl(" cover.png "); request.setReadTimeMinutes(null);
        var response = service.createBlog(principal, request);
        assertEquals(10L, response.getId()); assertEquals("Chăm sóc Động vật", response.getTitle()); assertEquals("cham-soc-dong-vat", response.getSlug()); assertNull(response.getSummary()); assertEquals("cover.png", response.getCoverImageUrl()); assertEquals(1, response.getReadTimeMinutes()); assertNull(response.getPublishedAt()); assertEquals(1L, response.getAuthorId());
    }

    @Test
    void createPublishedRetriesDuplicateSlugAndSetsPublishedTime() {
        when(users.findById(1L)).thenReturn(Optional.of(user(1L))); when(blogs.existsBySlug("hello")).thenReturn(true); when(blogs.existsBySlug("hello-2")).thenReturn(false); when(blogs.save(any(Blog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.createBlog(principal, createRequest("Hello", " hello ", Blog.BlogStatus.published));
        assertEquals("hello-2", response.getSlug()); assertEquals("published", response.getStatus()); assertNotNull(response.getPublishedAt());
    }

    @Test
    void createRejectsMissingAuthorBlankTitleContentAndInvalidSlug() {
        when(users.findById(1L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.createBlog(principal, createRequest("Title", null, Blog.BlogStatus.draft)));
        when(users.findById(1L)).thenReturn(Optional.of(user(1L))); AdminCreateBlogRequest blankTitle = createRequest(" ", null, Blog.BlogStatus.draft); assertThrows(BadRequestException.class, () -> service.createBlog(principal, blankTitle));
        AdminCreateBlogRequest blankContent = createRequest("Title", null, Blog.BlogStatus.draft); blankContent.setContent(" "); assertThrows(BadRequestException.class, () -> service.createBlog(principal, blankContent));
        assertThrows(BadRequestException.class, () -> service.createBlog(principal, createRequest("Title", "@@@", Blog.BlogStatus.draft)));
    }

    @Test
    void updateMapsAllFieldsPublishesDraftAndKeepsPublishedTimestampForLaterStatusChanges() {
        Blog blog = blog(5L, Blog.BlogStatus.draft); when(blogs.findById(5L)).thenReturn(Optional.of(blog)); when(blogs.save(blog)).thenReturn(blog);
        AdminUpdateBlogRequest update = new AdminUpdateBlogRequest(); update.setTitle(" New "); update.setSlug("new-slug"); update.setSummary(" "); update.setContent(" Updated "); update.setCoverImageUrl(" "); update.setCategory(Blog.BlogCategory.vaccination); update.setReadTimeMinutes(0); update.setStatus(Blog.BlogStatus.published);
        var response = service.updateBlog(5L, update);
        assertEquals("New", response.getTitle()); assertEquals("new-slug", response.getSlug()); assertNull(response.getSummary()); assertEquals("Updated", response.getContent()); assertNull(response.getCoverImageUrl()); assertEquals("vaccination", response.getCategory()); assertEquals(1, response.getReadTimeMinutes()); assertNotNull(response.getPublishedAt());
        var publishedAt = blog.getPublishedAt(); AdminUpdateBlogRequest archive = new AdminUpdateBlogRequest(); archive.setStatus(Blog.BlogStatus.archived); service.updateBlog(5L, archive); assertEquals(publishedAt, blog.getPublishedAt());
    }

    @Test
    void updateAllowsCurrentSlugAndRejectsMissingBlogAndInvalidOptionalFields() {
        Blog blog = blog(6L, Blog.BlogStatus.published); blog.setSlug("same"); when(blogs.findById(6L)).thenReturn(Optional.of(blog)); when(blogs.existsBySlug("same")).thenReturn(true); when(blogs.save(blog)).thenReturn(blog);
        AdminUpdateBlogRequest sameSlug = new AdminUpdateBlogRequest(); sameSlug.setSlug("same"); assertEquals("same", service.updateBlog(6L, sameSlug).getSlug());
        when(blogs.findById(9L)).thenReturn(Optional.empty()); assertThrows(ResourceNotFoundException.class, () -> service.updateBlog(9L, new AdminUpdateBlogRequest()));
        AdminUpdateBlogRequest blank = new AdminUpdateBlogRequest(); blank.setTitle(" "); assertThrows(BadRequestException.class, () -> service.updateBlog(6L, blank)); blank.setTitle(null); blank.setContent(" "); assertThrows(BadRequestException.class, () -> service.updateBlog(6L, blank));
    }

    private AdminCreateBlogRequest createRequest(String title, String slug, Blog.BlogStatus status) { AdminCreateBlogRequest request = new AdminCreateBlogRequest(); request.setTitle(title); request.setSlug(slug); request.setContent("Content"); request.setCategory(Blog.BlogCategory.health); request.setStatus(status); return request; }
    private Blog blog(Long id, Blog.BlogStatus status) { Blog blog = new Blog(); blog.setId(id); blog.setAuthor(user(7L)); blog.setTitle("Title"); blog.setSlug("title"); blog.setContent("Content"); blog.setCategory(Blog.BlogCategory.health); blog.setStatus(status); blog.setReadTimeMinutes(3); return blog; }
    private User user(Long id) { User user = new User(); user.setId(id); user.setFullName("Admin"); user.setEmail("admin@example.com"); return user; }
}
