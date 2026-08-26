package com.petcare.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.petcare.backend.dto.post.response.PetSummaryResponse;
import com.petcare.backend.dto.post.response.ReactionSummaryResponse;
import com.petcare.backend.model.CommentMedia;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.PostMedia;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.MediaType;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostMapperImplTest {
    private final PostMapperImpl mapper = new PostMapperImpl();

    @Test
    void toPostResponseMapsCompletePostPreservesMediaOrderAndGrantsOwnerPermissions() {
        User author = user(7L, "Ngọc", "ngoc@example.com", "avatar.png");
        Post post = post(3L, author);
        post.setCaption("Hello pets");
        post.setPrivacy(PostPrivacy.FRIENDS);
        post.setStatus(PostStatus.PUBLISHED);
        post.setCommentsLocked(true);
        LocalDateTime created = LocalDateTime.of(2026, 7, 1, 10, 0);
        post.setCreatedAt(created); post.setUpdatedAt(created.plusHours(1));
        PostMedia first = postMedia(1L, MediaType.IMAGE, "one.png", 2);
        PostMedia second = postMedia(2L, MediaType.VIDEO, "two.mp4", 1);
        ReactionSummaryResponse reactions = ReactionSummaryResponse.builder().total(5).like(3)
                .currentUserReaction("love").build();
        List<PetSummaryResponse> pets = List.of(PetSummaryResponse.builder().id(11L).name("Milo").build());

        var response = mapper.toPostResponse(post, List.of(first, second), pets, reactions, 4L, true, 7L);

        assertEquals(3L, response.getId()); assertEquals(7L, response.getUserId());
        assertEquals("Ngọc", response.getAuthorName()); assertEquals("avatar.png", response.getAuthorAvatarUrl());
        assertSame(pets, response.getPets()); assertEquals("Hello pets", response.getCaption());
        assertEquals("friends", response.getPrivacy()); assertEquals("published", response.getStatus());
        assertTrue(response.getCommentsLocked()); assertEquals(2, response.getMedia().size());
        assertEquals("one.png", response.getMedia().get(0).getMediaUrl());
        assertEquals("two.mp4", response.getMedia().get(1).getMediaUrl());
        assertSame(reactions, response.getReactions()); assertEquals(4L, response.getCommentCount());
        assertTrue(response.isReactedByCurrentUser()); assertEquals("love", response.getCurrentUserReaction());
        assertTrue(response.getSavedByCurrentUser()); assertTrue(response.isCanEdit()); assertTrue(response.isCanDelete());
        assertEquals(created, response.getCreatedAt()); assertEquals(created.plusHours(1), response.getUpdatedAt());
        assertEquals(2, first.getDisplayOrder(), "mapping must not mutate input media");
    }

    @Test
    void toPostResponseHandlesNullCollectionsAnonymousAuthorAndNullOptionalFields() {
        Post post = post(4L, null);
        post.setCaption(null); post.setPrivacy(null); post.setStatus(null); post.setCommentsLocked(null);

        var response = mapper.toPostResponse(post, null, null, null, 0L, false, null);

        assertNull(response.getUserId()); assertNull(response.getAuthorName()); assertNull(response.getAuthorAvatarUrl());
        assertTrue(response.getPets().isEmpty()); assertTrue(response.getMedia().isEmpty());
        assertNull(response.getPrivacy()); assertNull(response.getStatus()); assertNull(response.getCommentsLocked());
        assertEquals(0L, response.getReactions().getTotal()); assertFalse(response.isReactedByCurrentUser());
        assertNull(response.getCurrentUserReaction()); assertFalse(response.getSavedByCurrentUser());
        assertFalse(response.isCanEdit()); assertFalse(response.isCanDelete());
    }

    @Test
    void mediaMappersMapAllFieldsAndSupportNullMediaType() {
        LocalDateTime created = LocalDateTime.of(2026, 7, 2, 9, 0);
        PostMedia postMedia = postMedia(8L, null, "file.bin", null);
        postMedia.setThumbnailUrl("thumb"); postMedia.setOriginalFilename("file.bin");
        postMedia.setMimeType("application/octet-stream"); postMedia.setFileSize(99L); postMedia.setAltText("alt"); postMedia.setCreatedAt(created);
        var postResponse = mapper.toPostMediaResponse(postMedia);
        assertEquals(8L, postResponse.getId()); assertNull(postResponse.getMediaType());
        assertEquals("file.bin", postResponse.getMediaUrl()); assertEquals("thumb", postResponse.getThumbnailUrl());
        assertEquals("file.bin", postResponse.getOriginalFilename()); assertEquals("application/octet-stream", postResponse.getMimeType());
        assertEquals(99L, postResponse.getFileSize()); assertNull(postResponse.getDisplayOrder());
        assertEquals("alt", postResponse.getAltText()); assertEquals(created, postResponse.getCreatedAt());

        CommentMedia commentMedia = new CommentMedia(); commentMedia.setId(9L); commentMedia.setMediaType(MediaType.IMAGE);
        commentMedia.setMediaUrl("comment.png"); commentMedia.setDisplayOrder(0);
        var commentResponse = mapper.toCommentMediaResponse(commentMedia);
        assertEquals(9L, commentResponse.getId()); assertEquals("image", commentResponse.getMediaType());
        assertEquals("comment.png", commentResponse.getMediaUrl()); assertEquals(0, commentResponse.getDisplayOrder());
    }

    @Test
    void commentMappingCoversAuthorFallbacksPermissionAndMediaPartitions() {
        Post post = post(15L, user(99L, "Owner", "owner@example.com", null));
        User emailOnly = user(8L, "  ", "email@example.com", null);
        PostComment comment = comment(20L, post, emailOnly);
        comment.setParentCommentId(2L); comment.setRootCommentId(1L); comment.setDepth(1); comment.setCommentText("reply");
        comment.setStatus(CommentStatus.VISIBLE);
        CommentMedia media = new CommentMedia(); media.setId(6L); media.setMediaType(MediaType.VIDEO); media.setMediaUrl("reply.mp4");
        ReactionSummaryResponse reactions = ReactionSummaryResponse.builder().currentUserReaction("haha").build();

        var ownerResponse = mapper.toCommentResponse(comment, List.of(media), reactions, 2L, 99L, 99L);
        assertEquals(15L, ownerResponse.getPostId()); assertEquals(8L, ownerResponse.getUserId());
        assertEquals("email@example.com", ownerResponse.getAuthorName()); assertEquals("visible", ownerResponse.getStatus());
        assertTrue(ownerResponse.isCanDelete()); assertEquals(1, ownerResponse.getMedia().size());
        assertEquals("video", ownerResponse.getMedia().get(0).getMediaType()); assertTrue(ownerResponse.isReactedByCurrentUser());

        User fallback = user(10L, " ", " ", null);
        PostComment fallbackComment = comment(21L, null, fallback); fallbackComment.setStatus(null);
        var nonOwner = mapper.toCommentResponse(fallbackComment, null, null, 0L, 99L, 77L);
        assertNull(nonOwner.getPostId()); assertEquals("User 10", nonOwner.getAuthorName()); assertNull(nonOwner.getStatus());
        assertTrue(nonOwner.getMedia().isEmpty()); assertEquals(0L, nonOwner.getReactions().getTotal());
        assertFalse(nonOwner.isCanDelete()); assertFalse(nonOwner.isReactedByCurrentUser());
    }

    @Test
    void simpleCommentOverloadUsesEmptySocialDataAndAuthorCanDeleteOwnComment() {
        PostComment comment = comment(30L, post(3L, null), user(5L, "Author", "a@example.com", "a.png"));
        var response = mapper.toCommentResponse(comment, 3L, 5L);
        assertTrue(response.isCanDelete()); assertTrue(response.getMedia().isEmpty()); assertTrue(response.getReplies().isEmpty());
        assertEquals(0L, response.getReplyCount()); assertEquals(0L, response.getReactions().getTotal());
    }

    private User user(Long id, String name, String email, String avatar) { User user = new User(); user.setId(id); user.setFullName(name); user.setEmail(email); user.setAvatarUrl(avatar); return user; }
    private Post post(Long id, User author) { Post post = new Post(); post.setId(id); post.setUser(author); return post; }
    private PostMedia postMedia(Long id, MediaType type, String url, Integer order) { PostMedia media = new PostMedia(); media.setId(id); media.setMediaType(type); media.setMediaUrl(url); media.setDisplayOrder(order); return media; }
    private PostComment comment(Long id, Post post, User author) { PostComment comment = new PostComment(); comment.setId(id); comment.setPost(post); comment.setUser(author); return comment; }
}
