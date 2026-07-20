package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.response.PostCommentResponse;
import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ForbiddenException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.repository.CommentMediaRepository;
import com.petcare.backend.repository.CommentReactionRepository;
import com.petcare.backend.repository.PostCommentRepository;
import com.petcare.backend.repository.PostRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.FileStorageService;
import com.petcare.backend.service.PostMapper;
import com.petcare.backend.service.SocialNotificationService;
import com.petcare.backend.service.SocialPermissionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceImplTest {
    @Mock private PostRepository postRepository;
    @Mock private PostCommentRepository postCommentRepository;
    @Mock private CommentMediaRepository commentMediaRepository;
    @Mock private CommentReactionRepository commentReactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private PostMapper postMapper;
    @Mock private SocialPermissionService socialPermissionService;
    @Mock private SocialNotificationService socialNotificationService;

    private CommentServiceImpl service;
    private User owner;
    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        service = new CommentServiceImpl(postRepository, postCommentRepository, commentMediaRepository,
                commentReactionRepository, userRepository, fileStorageService, postMapper,
                socialPermissionService, socialNotificationService);
        owner = user(1L, "user");
        author = user(2L, "user");
        post = post(owner, PostStatus.PUBLISHED, false);
        when(commentMediaRepository.findByComment_IdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(postCommentRepository.findByParentCommentIdAndStatusOrderByCreatedAtAsc(any(), eq(CommentStatus.VISIBLE)))
                .thenReturn(List.of());
        when(commentReactionRepository.findByComment_IdAndUser_Id(any(), any())).thenReturn(Optional.empty());
    }

    // EP: valid text-only root comment.
    @Test
    void createCommentWithFiles_ValidRootComment_SavesNormalizedCommentAndNotifiesPost() {
        allowCreate();
        when(postCommentRepository.save(any(PostComment.class))).thenAnswer(invocation -> {
            PostComment comment = invocation.getArgument(0);
            if (comment.getId() == null) comment.setId(10L);
            return comment;
        });
        when(fileStorageService.storeCommentMediaFiles(null)).thenReturn(List.of());
        stubResponse();

        PostCommentResponse result = service.createCommentWithFiles(5L, 2L, "  hello  ", null, null);

        ArgumentCaptor<PostComment> commentCaptor = ArgumentCaptor.forClass(PostComment.class);
        verify(postCommentRepository, org.mockito.Mockito.atLeastOnce()).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().getDepth()).isZero();
        assertThat(commentCaptor.getValue().getCommentText()).isEqualTo("hello");
        verify(socialNotificationService).notifyPostComment(eq(post), any(PostComment.class), eq(author));
        assertThat(result.getId()).isEqualTo(10L);
    }

    // EP/BVA: direct reply to a visible root comment (depth 0) is allowed.
    @Test
    void createCommentWithFiles_ValidReply_CreatesDepthOneAndNotifiesReplyAuthor() {
        allowCreate();
        PostComment parent = comment(20L, user(3L, "user"), post, 0, CommentStatus.VISIBLE);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(parent));
        when(postCommentRepository.save(any(PostComment.class))).thenAnswer(invocation -> {
            PostComment value = invocation.getArgument(0); value.setId(21L); return value;
        });
        when(fileStorageService.storeCommentMediaFiles(List.of())).thenReturn(List.of());
        stubResponse();

        service.createCommentWithFiles(5L, 2L, "reply", 20L, List.of());

        ArgumentCaptor<PostComment> captor = ArgumentCaptor.forClass(PostComment.class);
        verify(postCommentRepository).save(captor.capture());
        assertThat(captor.getValue().getParentCommentId()).isEqualTo(20L);
        assertThat(captor.getValue().getRootCommentId()).isEqualTo(20L);
        assertThat(captor.getValue().getDepth()).isEqualTo(1);
        verify(socialNotificationService).notifyCommentReply(eq(parent), any(PostComment.class), eq(author));
    }

    // BVA: depth 2 is the first disallowed parent depth.
    @Test
    void createCommentWithFiles_ParentAtMaxDepth_ThrowsBadRequest() {
        allowCreate();
        PostComment parent = comment(20L, owner, post, 2, CommentStatus.VISIBLE);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> service.createCommentWithFiles(5L, 2L, "reply", 20L, List.of()))
                .isInstanceOf(BadRequestException.class).hasMessage("Only two-level replies are supported");
    }

    // EP: hidden and locked posts reject comments after the post is found.
    @Test
    void createCommentWithFiles_HiddenPost_ThrowsForbidden() {
        when(postRepository.findByIdAndStatusNot(5L, PostStatus.DELETED))
                .thenReturn(Optional.of(post(owner, PostStatus.HIDDEN, false)));
        doNothing().when(socialPermissionService).checkUserActive(2L);

        assertThatThrownBy(() -> service.createCommentWithFiles(5L, 2L, "text", null, List.of()))
                .isInstanceOf(ForbiddenException.class).hasMessage("Cannot comment on hidden post");
    }

    // BVA: ten media files are accepted; eleven are rejected.
    @Test
    void createCommentWithFiles_ElevenFiles_ThrowsBadRequest() {
        allowCreate();
        List files = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> new MockMultipartFile("files", new byte[] {1})).toList();

        assertThatThrownBy(() -> service.createCommentWithFiles(5L, 2L, "text", null, files))
                .isInstanceOf(BadRequestException.class).hasMessage("A comment can contain at most 10 files");
        verify(postCommentRepository, never()).save(any());
    }

    // EP: blank text with no media belongs to the invalid comment-content partition.
    @Test
    void createCommentWithFiles_BlankTextAndNoFiles_ThrowsBadRequest() {
        allowCreate();
        assertThatThrownBy(() -> service.createCommentWithFiles(5L, 2L, "   ", null, List.of()))
                .isInstanceOf(BadRequestException.class).hasMessage("Comment must contain text or media");
    }

    // BVA: page=0, size=51 caps the repository pageable at 50.
    @Test
    void getPostComments_SizeAboveMaximum_CapsPageSize() {
        allowViewPost();
        when(postCommentRepository.findByPost_IdAndParentCommentIdIsNullAndStatusOrderByCreatedAtDesc(
                eq(5L), eq(CommentStatus.VISIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 50), 0));

        PageResponse<PostCommentResponse> result = service.getPostComments(5L, 2L, 0, 51);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postCommentRepository).findByPost_IdAndParentCommentIdIsNullAndStatusOrderByCreatedAtDesc(
                eq(5L), eq(CommentStatus.VISIBLE), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(result.getSize()).isEqualTo(50);
    }

    // BVA: page=-1 and size=0 are invalid paging boundaries.
    @Test
    void getPostComments_InvalidPaging_ThrowsBadRequest() {
        allowViewPost();
        assertThatThrownBy(() -> service.getPostComments(5L, 2L, -1, 1))
                .isInstanceOf(BadRequestException.class).hasMessage("Page must not be negative");
        assertThatThrownBy(() -> service.getPostComments(5L, 2L, 0, 0))
                .isInstanceOf(BadRequestException.class).hasMessage("Size must be greater than 0");
    }

    // EP: only visible comments can be used as a replies root.
    @Test
    void getCommentReplies_DeletedComment_ThrowsNotFound() {
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(comment(20L, author, post, 0, CommentStatus.DELETED)));

        assertThatThrownBy(() -> service.getCommentReplies(20L, 2L, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Comment not found");
    }

    // EP: author may update text; null files retains existing media.
    @Test
    void updateCommentWithFiles_AuthorUpdatesText_NormalizesAndSaves() {
        PostComment existing = comment(20L, author, post, 0, CommentStatus.VISIBLE);
        existing.setCommentText("old");
        allowUpdate(existing, author);
        when(postCommentRepository.save(existing)).thenReturn(existing);
        stubResponse();

        PostCommentResponse response = service.updateCommentWithFiles(20L, 2L, " new text ", null);

        assertThat(existing.getCommentText()).isEqualTo("new text");
        assertThat(response.getId()).isEqualTo(20L);
        verify(commentMediaRepository, never()).deleteByComment_Id(any());
    }

    // EP: admin is an authorized non-author; replacement files delete old metadata and save new metadata.
    @Test
    void updateCommentWithFiles_AdminWithMedia_ReplacesMedia() {
        User admin = user(3L, "ADMIN");
        PostComment existing = comment(20L, author, post, 0, CommentStatus.VISIBLE);
        existing.setCommentText("old");
        doNothing().when(socialPermissionService).checkUserActive(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin));
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(postCommentRepository.save(existing)).thenReturn(existing);
        List files = List.of(new MockMultipartFile("files", new byte[] {1}));
        when(fileStorageService.storeCommentMediaFiles(files)).thenReturn(List.of(UploadFileResponse.builder()
                .mediaType("image").mediaUrl("url").originalFilename("cat.jpg").mimeType("image/jpeg").fileSize(1L).build()));
        stubResponse();

        service.updateCommentWithFiles(20L, 3L, null, files);

        verify(commentMediaRepository).deleteByComment_Id(20L);
        verify(commentMediaRepository).saveAll(any());
    }

    // EP: an unrelated standard user may not update a visible comment.
    @Test
    void updateCommentWithFiles_NonAuthorNonAdmin_ThrowsForbidden() {
        PostComment existing = comment(20L, author, post, 0, CommentStatus.VISIBLE);
        allowUpdate(existing, user(3L, "user"));

        assertThatThrownBy(() -> service.updateCommentWithFiles(20L, 3L, "text", null))
                .isInstanceOf(ForbiddenException.class).hasMessage("You do not have permission to update this comment");
    }

    // EP: update cannot leave a comment with neither text nor media.
    @Test
    void updateCommentWithFiles_EmptyTextAndNoExistingMedia_ThrowsBadRequest() {
        PostComment existing = comment(20L, author, post, 0, CommentStatus.VISIBLE);
        existing.setCommentText("");
        allowUpdate(existing, author);

        assertThatThrownBy(() -> service.updateCommentWithFiles(20L, 2L, " ", List.of()))
                .isInstanceOf(BadRequestException.class).hasMessage("Comment must contain text or media");
    }

    // EP: deleting an already deleted comment is idempotent.
    @Test
    void deleteComment_AlreadyDeleted_ReturnsWithoutMutation() {
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(comment(20L, author, post, 0, CommentStatus.DELETED)));

        service.deleteComment(20L, 2L);

        verify(postCommentRepository, never()).saveAll(any());
        verify(commentReactionRepository, never()).deleteByComment_IdIn(any());
    }

    // EP: root comment deletion includes all visible descendants and removes their reactions.
    @Test
    void deleteComment_RootAuthor_DeletesVisibleBranch() {
        PostComment root = comment(20L, author, post, 0, CommentStatus.VISIBLE);
        PostComment reply = comment(21L, owner, post, 1, CommentStatus.VISIBLE);
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(root));
        when(postCommentRepository.findByRootCommentIdAndStatusOrderByCreatedAtAsc(20L, CommentStatus.VISIBLE))
                .thenReturn(List.of(reply));
        when(postCommentRepository.findAllById(List.of(20L, 21L))).thenReturn(List.of(root, reply));

        service.deleteComment(20L, 2L);

        assertThat(root.getStatus()).isEqualTo(CommentStatus.DELETED);
        assertThat(reply.getStatus()).isEqualTo(CommentStatus.DELETED);
        verify(commentReactionRepository).deleteByComment_IdIn(List.of(20L, 21L));
    }

    // EP: non-owner and non-author cannot delete.
    @Test
    void deleteComment_UnrelatedUser_ThrowsForbidden() {
        PostComment comment = comment(20L, author, post, 1, CommentStatus.VISIBLE);
        doNothing().when(socialPermissionService).checkUserActive(3L);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.deleteComment(20L, 3L))
                .isInstanceOf(ForbiddenException.class).hasMessage("You do not have permission to delete this comment");
    }

    // BVA: null, zero and negative identifiers are outside the valid ID partition.
    @Test
    void publicMethods_InvalidIdentifiers_ThrowBadRequest() {
        assertThatThrownBy(() -> service.createCommentWithFiles(null, 2L, "text", null, List.of()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getPostComments(0L, 2L, 0, 1))
                .isInstanceOf(BadRequestException.class);
        doNothing().when(socialPermissionService).checkUserActive(2L);
        assertThatThrownBy(() -> service.getCommentReplies(-1L, 2L, 0, 1))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.deleteComment(null, 2L))
                .isInstanceOf(BadRequestException.class);
    }

    // EP: a locked published post is not commentable.
    @Test
    void createCommentWithFiles_LockedPost_ThrowsConflict() {
        Post locked = post(owner, PostStatus.PUBLISHED, true);
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postRepository.findByIdAndStatusNot(5L, PostStatus.DELETED)).thenReturn(Optional.of(locked));
        doNothing().when(socialPermissionService).checkCanViewPost(2L, locked);

        assertThatThrownBy(() -> service.createCommentWithFiles(5L, 2L, "text", null, List.of()))
                .isInstanceOf(com.petcare.backend.exception.ConflictException.class)
                .hasMessage("Comments are locked for this post");
    }

    // EP: parents from another post and non-visible parents are invalid reply targets.
    @Test
    void createCommentWithFiles_InvalidParent_ThrowsBadRequest() {
        allowCreate();
        Post anotherPost = post(owner, PostStatus.PUBLISHED, false); anotherPost.setId(6L);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(comment(20L, owner, anotherPost, 0, CommentStatus.VISIBLE)));
        assertThatThrownBy(() -> service.createCommentWithFiles(5L, 2L, "reply", 20L, List.of()))
                .isInstanceOf(BadRequestException.class).hasMessage("Parent comment does not belong to this post");

        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(comment(20L, owner, post, 0, CommentStatus.DELETED)));
        assertThatThrownBy(() -> service.createCommentWithFiles(5L, 2L, "reply", 20L, List.of()))
                .isInstanceOf(BadRequestException.class).hasMessage("Parent comment is not visible");
    }

    // EP: non-empty uploads create ordered media metadata.
    @Test
    void createCommentWithFiles_MediaOnlyComment_PersistsMedia() {
        allowCreate();
        when(postCommentRepository.save(any(PostComment.class))).thenAnswer(invocation -> {
            PostComment value = invocation.getArgument(0); if (value.getId() == null) value.setId(10L); return value;
        });
        List files = List.of(new MockMultipartFile("files", new byte[] {1}));
        when(fileStorageService.storeCommentMediaFiles(files)).thenReturn(List.of(UploadFileResponse.builder()
                .mediaType("image").mediaUrl("image-url").thumbnailUrl("thumb").originalFilename("cat.jpg")
                .mimeType("image/jpeg").fileSize(10L).build()));
        stubResponse();

        service.createCommentWithFiles(5L, 2L, null, null, files);

        verify(commentMediaRepository).saveAll(any());
    }

    // EP: a visible replies root returns a page and maps its content.
    @Test
    void getCommentReplies_VisibleComment_ReturnsPageResponse() {
        PostComment root = comment(20L, author, post, 0, CommentStatus.VISIBLE);
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(root));
        doNothing().when(socialPermissionService).checkCanViewPost(2L, post);
        PostComment reply = comment(21L, owner, post, 1, CommentStatus.VISIBLE);
        when(postCommentRepository.findByParentCommentIdAndStatusOrderByCreatedAtAsc(
                eq(20L), eq(CommentStatus.VISIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reply)));
        stubResponse();

        PageResponse<PostCommentResponse> response = service.getCommentReplies(20L, 2L, 0, 20);

        assertThat(response.getContent()).hasSize(1);
    }

    // EP: a missing post, comment or user produces a resource-not-found response.
    @Test
    void publicMethods_MissingResources_ThrowNotFound() {
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postRepository.findByIdAndStatusNot(5L, PostStatus.DELETED)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPostComments(5L, 2L, 0, 1)).isInstanceOf(ResourceNotFoundException.class);

        when(postRepository.findByIdAndStatusNot(5L, PostStatus.DELETED)).thenReturn(Optional.of(post));
        doNothing().when(socialPermissionService).checkCanViewPost(2L, post);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createCommentWithFiles(5L, 2L, "text", null, List.of()))
                .isInstanceOf(ResourceNotFoundException.class);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteComment(20L, 2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // EP: deleting a reply uses the child-list branch rather than root-list branch.
    @Test
    void deleteComment_ReplyAuthor_DeletesReplyAndVisibleChildren() {
        PostComment reply = comment(20L, author, post, 1, CommentStatus.VISIBLE);
        PostComment child = comment(21L, owner, post, 2, CommentStatus.VISIBLE);
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(reply));
        when(postCommentRepository.findByParentCommentIdAndStatusOrderByCreatedAtAsc(20L, CommentStatus.VISIBLE))
                .thenReturn(List.of(child));
        when(postCommentRepository.findAllById(List.of(20L, 21L))).thenReturn(List.of(reply, child));

        service.deleteComment(20L, 2L);

        verify(commentReactionRepository).deleteByComment_IdIn(List.of(20L, 21L));
    }

    // EP: nullable parent depth/root values are treated as a root parent, and a null parent author is notified.
    @Test
    void createCommentWithFiles_ParentWithNullDepthAndRoot_CreatesFirstLevelReply() {
        allowCreate();
        PostComment parent = comment(20L, null, post, null, CommentStatus.VISIBLE);
        parent.setRootCommentId(null);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(parent));
        when(postCommentRepository.save(any(PostComment.class))).thenAnswer(invocation -> {
            PostComment value = invocation.getArgument(0); value.setId(21L); return value;
        });
        when(fileStorageService.storeCommentMediaFiles(List.of())).thenReturn(null);
        stubResponse();

        service.createCommentWithFiles(5L, 2L, "reply", 20L, List.of());

        verify(socialNotificationService).notifyCommentReply(eq(parent), any(), eq(author));
    }

    // EP: an update with null text retains existing text and existing media.
    @Test
    void updateCommentWithFiles_NullTextWithExistingMedia_RetainsContent() {
        PostComment existing = comment(20L, author, post, 2, CommentStatus.VISIBLE);
        allowUpdate(existing, author);
        when(commentMediaRepository.findByComment_IdOrderByDisplayOrderAsc(20L))
                .thenReturn(List.of(com.petcare.backend.model.CommentMedia.builder().build()));
        when(postCommentRepository.save(existing)).thenReturn(existing);
        stubResponse();

        service.updateCommentWithFiles(20L, 2L, null, null);

        verify(postCommentRepository).save(existing);
    }

    // EP: post ownership may be absent; the comment author can still delete their comment.
    @Test
    void deleteComment_AuthorWithMissingPostOwner_DeletesComment() {
        Post postWithoutOwner = post(null, PostStatus.PUBLISHED, false);
        PostComment value = comment(20L, author, postWithoutOwner, 1, CommentStatus.VISIBLE);
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(value));
        when(postCommentRepository.findByParentCommentIdAndStatusOrderByCreatedAtAsc(20L, CommentStatus.VISIBLE))
                .thenReturn(List.of());
        when(postCommentRepository.findAllById(List.of(20L))).thenReturn(List.of(value));

        service.deleteComment(20L, 2L);

        assertThat(value.getStatus()).isEqualTo(CommentStatus.DELETED);
    }

    // EP: comment ownership may be absent; the post owner can still delete the comment.
    @Test
    void deleteComment_PostOwnerWithMissingCommentAuthor_DeletesComment() {
        PostComment value = comment(20L, null, post, 1, CommentStatus.VISIBLE);
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(value));
        when(postCommentRepository.findByParentCommentIdAndStatusOrderByCreatedAtAsc(20L, CommentStatus.VISIBLE))
                .thenReturn(List.of());
        when(postCommentRepository.findAllById(List.of(20L))).thenReturn(List.of(value));

        service.deleteComment(20L, 1L);

        assertThat(value.getStatus()).isEqualTo(CommentStatus.DELETED);
    }

    // EP: response construction tolerates a missing post owner.
    @Test
    void updateCommentWithFiles_PostWithoutOwner_ReturnsResponse() {
        PostComment existing = comment(20L, author, post(null, PostStatus.PUBLISHED, false), 0, CommentStatus.VISIBLE);
        allowUpdate(existing, author);
        when(postCommentRepository.save(existing)).thenReturn(existing);
        stubResponse();

        PostCommentResponse response = service.updateCommentWithFiles(20L, 2L, "changed", null);

        assertThat(response.getId()).isEqualTo(20L);
    }

    private void allowCreate() {
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postRepository.findByIdAndStatusNot(5L, PostStatus.DELETED)).thenReturn(Optional.of(post));
        doNothing().when(socialPermissionService).checkCanViewPost(2L, post);
        when(userRepository.findById(2L)).thenReturn(Optional.of(author));
    }

    private void allowViewPost() {
        doNothing().when(socialPermissionService).checkUserActive(2L);
        when(postRepository.findByIdAndStatusNot(5L, PostStatus.DELETED)).thenReturn(Optional.of(post));
        doNothing().when(socialPermissionService).checkCanViewPost(2L, post);
    }

    private void allowUpdate(PostComment existing, User currentUser) {
        doNothing().when(socialPermissionService).checkUserActive(currentUser.getId());
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(postCommentRepository.findById(20L)).thenReturn(Optional.of(existing));
        doNothing().when(socialPermissionService).checkCanViewPost(currentUser.getId(), post);
    }

    private void stubResponse() {
        when(postMapper.toCommentResponse(any(), any(), any(), any(Long.class), any(), any()))
                .thenAnswer(invocation -> PostCommentResponse.builder().id(((PostComment) invocation.getArgument(0)).getId()).build());
    }

    private User user(Long id, String role) {
        User user = new User(); user.setId(id); user.setRole(role); user.setEmail("u" + id + "@example.test"); return user;
    }

    private Post post(User postOwner, PostStatus status, boolean locked) {
        Post value = new Post(); value.setId(5L); value.setUser(postOwner); value.setStatus(status); value.setCommentsLocked(locked); return value;
    }

    private PostComment comment(Long id, User commentAuthor, Post commentPost, Integer depth, CommentStatus status) {
        PostComment value = new PostComment();
        value.setId(id); value.setUser(commentAuthor); value.setPost(commentPost); value.setDepth(depth);
        value.setRootCommentId(depth == null || depth == 0 ? id : 20L); value.setStatus(status); value.setCommentText("text");
        return value;
    }
}
