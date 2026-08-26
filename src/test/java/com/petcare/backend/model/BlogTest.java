package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BlogTest {
    @Test
    void accessors_DefaultsAndPersistCallbacks_HandlePublishedAndNonPublishedStates() {
        Blog blog = new Blog(); User author = new User(); author.setId(1L);
        blog.setId(2L); blog.setAuthor(author); blog.setTitle("Title"); blog.setSlug("title"); blog.setSummary("");
        blog.setContent("Content"); blog.setCoverImageUrl(null); blog.setCategory(Blog.BlogCategory.health);
        assertThat(blog.getStatus()).isEqualTo(Blog.BlogStatus.draft); assertThat(blog.getReadTimeMinutes()).isEqualTo(1);
        blog.setStatus(Blog.BlogStatus.published); blog.setReadTimeMinutes(0); blog.prePersist();
        assertThat(blog.getAuthor()).isSameAs(author); assertThat(blog.getId()).isEqualTo(2L);
        assertThat(blog.getPublishedAt()).isNotNull(); assertThat(blog.getCreatedAt()).isNotNull(); assertThat(blog.getUpdatedAt()).isNotNull();

        LocalDateTime publishedAt = blog.getPublishedAt(); LocalDateTime beforeUpdate = blog.getUpdatedAt();
        blog.preUpdate();
        assertThat(blog.getPublishedAt()).isEqualTo(publishedAt); assertThat(blog.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
        Blog draft = new Blog(); draft.setStatus(Blog.BlogStatus.archived); draft.prePersist();
        assertThat(draft.getPublishedAt()).isNull();
        Blog publishOnUpdate = new Blog(); publishOnUpdate.setStatus(Blog.BlogStatus.published); publishOnUpdate.preUpdate();
        assertThat(publishOnUpdate.getPublishedAt()).isEqualTo(publishOnUpdate.getUpdatedAt());
        Blog alreadyPublished = new Blog(); alreadyPublished.setStatus(Blog.BlogStatus.published);
        LocalDateTime existingPublication = LocalDateTime.of(2025, 1, 1, 0, 0); alreadyPublished.setPublishedAt(existingPublication);
        alreadyPublished.prePersist(); assertThat(alreadyPublished.getPublishedAt()).isEqualTo(existingPublication);
        Blog archivedOnUpdate = new Blog(); archivedOnUpdate.setStatus(Blog.BlogStatus.archived); archivedOnUpdate.preUpdate();
        assertThat(archivedOnUpdate.getPublishedAt()).isNull();
    }
}
